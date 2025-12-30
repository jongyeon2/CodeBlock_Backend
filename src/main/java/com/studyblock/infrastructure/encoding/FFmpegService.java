package com.studyblock.infrastructure.encoding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FFmpeg 비디오 인코딩 서비스
 * - Spring Boot 컨테이너 내부에 설치된 FFmpeg 직접 실행
 * - 현재는 720p 단일 해상도만 인코딩 (속도 및 메모리 최적화)
 * - 720p 선택 이유: 1080p보다 약 30-40% 빠른 인코딩 속도, 작은 파일 크기, 우수한 스트리밍 성능
 * - 속도 최적화: preset=veryfast, threads=0 (자동)
 */
@Service
@Slf4j
public class FFmpegService {

    private static final int TIMEOUT_MINUTES = 30;
    
    // workspace 경로를 application.yml에서 주입받음
    @Value("${video.encoding.workspace:./tmp/videos}")
    private String workspacePath;

    // 병렬 인코딩용 ExecutorService (3개 해상도 동시 처리)
    private final ExecutorService encodingExecutor = Executors.newFixedThreadPool(3);

    /**
     * 비디오를 여러 해상도로 인코딩 (현재는 720p만 인코딩)
     *
     * 변경 사항:
     * - 720p만 인코딩하도록 변경 (속도 및 메모리 최적화)
     * - 이유: 720p가 1080p보다 인코딩 속도가 약 30-40% 빠르고, 파일 크기도 작음
     * - 스트리밍 성능이 더 좋고, 대부분의 사용자에게 충분한 품질 제공
     *
     * @param inputFileName 입력 파일명 (예: "video_123.mp4")
     * @return Map<해상도, 출력파일명> (예: {"1080p": null, "720p": "video_123_720p.mp4", "540p": null})
     * @throws IOException 인코딩 실패 시
     */
    public Map<String, String> encodeToMultipleResolutions(String inputFileName) throws IOException {
        log.info("720p 단일 해상도 인코딩 시작 - 입력 파일: {}", inputFileName);

        Map<String, String> encodedFiles = new java.util.HashMap<>();

        try {
            // 720p만 인코딩 (속도 최적화)
            log.info("720p 인코딩 시작");
            String outputFileName = encodeToResolution(inputFileName, "720");
            encodedFiles.put("720p", outputFileName);
            log.info("720p 인코딩 완료: {}", outputFileName);

            // 1080p, 540p는 null로 설정 (인코딩하지 않음)
            encodedFiles.put("1080p", null);
            encodedFiles.put("540p", null);

            log.info("720p 단일 해상도 인코딩 완료");
            return encodedFiles;

        } catch (IOException e) {
            log.error("720p 인코딩 실패 - 입력 파일: {}", inputFileName, e);
            throw new IOException("720p 인코딩 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 해상도로 비디오 인코딩
     *
     * @param inputFileName 입력 파일명
     * @param resolution 해상도 (현재는 720만 사용, 1080, 540는 미지원)
     * @return 출력 파일명
     * @throws IOException 인코딩 실패 시
     */
    private String encodeToResolution(String inputFileName, String resolution) throws IOException {
        String outputFileName = generateOutputFileName(inputFileName, resolution);
        String[] command = buildFFmpegCommand(inputFileName, resolution, outputFileName);

        log.info("{}p 인코딩 시작 - 입력: {}, 출력: {}", resolution, inputFileName, outputFileName);
        executeFFmpegCommand(command);
        return outputFileName;
    }

    /**
     * FFmpeg 명령어 빌드
     *
     * 최적화 옵션:
     * - preset: veryfast (fast 대비 2-3배 빠름)
     * - threads: 0 (자동 감지 - 모든 CPU 코어 활용)
     * - crf: 28 (23 대비 파일 크기 작고 인코딩 빠름, 품질 약간 낮춤)
     * 
     * 변경사항:
     * - Docker exec 제거: Spring Boot 컨테이너에 FFmpeg 직접 설치
     * - workspace 경로를 application.yml에서 주입받아 사용
     */
    private String[] buildFFmpegCommand(String inputFileName, String resolution, String outputFileName) {
        // workspace 경로를 절대 경로로 변환
        Path workspace = Paths.get(workspacePath).toAbsolutePath();
        String inputPath = workspace.resolve(inputFileName).toString();
        String outputPath = workspace.resolve(outputFileName).toString();
        
        return new String[]{
                "ffmpeg",  // Docker exec 제거, 직접 ffmpeg 실행
                "-i", inputPath,
                "-vf", "scale=-2:" + resolution,
                "-c:v", "libx264",
                "-crf", "28",           // 23 → 28 (속도 향상)
                "-preset", "veryfast",  // fast → veryfast (속도 2-3배 향상)
                "-c:a", "aac",
                "-b:a", "128k",
                "-threads", "0",        // 4 → 0 (자동 감지, 모든 CPU 코어 활용)
                "-y",
                outputPath
        };
    }

    /**
     * FFmpeg 명령어 실행
     * 
     * 개선사항:
     * - 프로세스 출력을 완전히 캡처하여 에러 메시지 손실 방지
     * - 종료 코드와 출력 내용을 함께 로깅
     * - 예외 발생 시 상세 정보 로깅
     */
    private void executeFFmpegCommand(String[] command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = null;
        CompletableFuture<String> outputFuture = null;
        
        try {
            String commandString = String.join(" ", command);
            log.info("FFmpeg 실행 명령어: {}", commandString);
            
            process = processBuilder.start();
            log.info("FFmpeg 프로세스 시작됨 - PID: {}, Alive: {}", 
                process.pid(), process.isAlive());
            
            outputFuture = captureProcessOutput(process);
            log.info("FFmpeg 출력 캡처 스레드 시작됨");

            // 프로세스가 정상적으로 시작되었는지 확인 (즉시 종료되었는지 체크)
            boolean immediatelyExited = !process.isAlive();
            if (immediatelyExited) {
                int exitCode = process.exitValue();
                String output = "";
                try {
                    output = outputFuture.get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("즉시 종료된 프로세스의 출력 읽기 실패", e);
                }
                log.error("FFmpeg 프로세스가 즉시 종료됨 - 종료 코드: {}, 출력: {}", exitCode, output);
                throw new IOException("FFmpeg 프로세스가 즉시 종료됨 - 종료 코드: " + exitCode + 
                    (output.isEmpty() ? "" : ", 출력: " + output.substring(0, Math.min(500, output.length()))));
            }

            log.info("FFmpeg 프로세스 실행 중 - 대기 시작 (최대 {}분)", TIMEOUT_MINUTES);
            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
            if (!finished) {
                log.error("FFmpeg 실행 시간 초과 ({}분) - 프로세스 강제 종료", TIMEOUT_MINUTES);
                process.destroyForcibly();
                String output = "";
                try {
                    output = outputFuture.get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("타임아웃된 프로세스의 출력 읽기 실패", e);
                }
                log.error("FFmpeg 실행 시간 초과 ({}분) - 출력: {}", TIMEOUT_MINUTES, output);
                throw new IOException("FFmpeg 실행 시간 초과 (" + TIMEOUT_MINUTES + "분)");
            }

            log.info("FFmpeg 프로세스 종료됨 - 종료 코드 확인 대기");
            
            // 프로세스 출력 읽기 완료 대기 (최대 5초)
            String output = "";
            try {
                output = outputFuture.get(5, TimeUnit.SECONDS);
                log.info("FFmpeg 출력 읽기 완료 - 출력 길이: {} bytes", output.length());
            } catch (TimeoutException e) {
                log.warn("프로세스 출력 읽기 타임아웃 (5초) - 부분 출력만 캡처됨");
            } catch (Exception e) {
                log.warn("프로세스 출력 읽기 실패", e);
            }

            int exitCode = process.exitValue();
            log.info("FFmpeg 종료 코드: {}", exitCode);
            
            if (exitCode != 0) {
                log.error("FFmpeg 실행 실패 - 종료 코드: {}, 명령어: {}, 출력: {}", 
                    exitCode, commandString, 
                    output.isEmpty() ? "(출력 없음)" : output.substring(0, Math.min(500, output.length())));
                throw new IOException("FFmpeg 실행 실패 - 종료 코드: " + exitCode + 
                    (output.isEmpty() ? "" : ", 출력: " + output.substring(0, Math.min(500, output.length()))));
            }
            log.info("FFmpeg 실행 완료 - 종료 코드: {}", exitCode);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("FFmpeg 실행 중단됨 - 명령어: {}", String.join(" ", command), e);
            throw new IOException("FFmpeg 실행 중단됨", e);
        } finally {
            if (process != null && process.isAlive()) {
                log.warn("FFmpeg 프로세스가 여전히 실행 중 - 강제 종료");
                process.destroyForcibly();
            }
        }
    }

    /**
     * 프로세스 출력 캡처 및 로깅
     * 
     * 개선사항:
     * - 프로세스 출력을 완전히 읽어서 StringBuilder에 저장
     * - 프로세스 종료 후에도 남은 출력 버퍼 읽기 보장
     * - 에러 메시지 즉시 로깅 (INFO 레벨)
     * - CompletableFuture로 반환하여 완료 대기 가능
     * 
     * @param process FFmpeg 프로세스
     * @return 프로세스 출력 내용을 담은 CompletableFuture
     */
    private CompletableFuture<String> captureProcessOutput(Process process) {
        CompletableFuture<String> future = new CompletableFuture<>();
        AtomicReference<StringBuilder> outputBuilder = new AtomicReference<>(new StringBuilder());
        
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                log.debug("FFmpeg 출력 읽기 스레드 시작");
                
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    String trimmedLine = line.trim();
                    
                    // 모든 출력 저장
                    outputBuilder.get().append(trimmedLine).append("\n");
                    
                    // 첫 번째 줄은 항상 로깅 (FFmpeg 시작 메시지 확인용)
                    if (lineCount == 1) {
                        log.info("FFmpeg 첫 출력: {}", trimmedLine);
                    }
                    
                    // 진행 상황 로깅 (INFO 레벨로 변경하여 항상 보이도록)
                    if (trimmedLine.contains("frame=") || trimmedLine.contains("time=") || 
                        trimmedLine.contains("speed=")) {
                        log.info("FFmpeg 진행: {}", trimmedLine);
                    }
                    // 입력 파일 관련 메시지 로깅
                    else if (trimmedLine.contains("Input #0") || 
                             trimmedLine.contains("Stream #") ||
                             trimmedLine.contains("Duration:") ||
                             trimmedLine.contains("bitrate:")) {
                        log.info("FFmpeg 입력 정보: {}", trimmedLine);
                    }
                    // 출력 파일 관련 메시지 로깅
                    else if (trimmedLine.contains("Output #0") ||
                             trimmedLine.contains("Stream mapping:") ||
                             trimmedLine.startsWith("Press [q]")) {
                        log.info("FFmpeg 출력 정보: {}", trimmedLine);
                    }
                    // 에러 메시지 즉시 로깅 (ERROR 레벨)
                    else if (trimmedLine.toLowerCase().contains("error") || 
                             trimmedLine.contains("failed") ||
                             trimmedLine.contains("cannot") ||
                             trimmedLine.contains("no such file") ||
                             trimmedLine.contains("permission denied") ||
                             trimmedLine.contains("invalid") ||
                             trimmedLine.contains("unable") ||
                             trimmedLine.contains("no space left") ||
                             trimmedLine.contains("read error") ||
                             trimmedLine.contains("write error")) {
                        log.error("FFmpeg Error: {}", trimmedLine);
                    }
                    // 일반적인 FFmpeg 출력도 처음 10줄은 INFO로 로깅 (헤더 정보 확인용)
                    else if (lineCount <= 10) {
                        log.info("FFmpeg 출력 [{}]: {}", lineCount, trimmedLine);
                    }
                    // 그 외의 모든 출력도 DEBUG로 로깅
                    else {
                        log.debug("FFmpeg 출력 [{}]: {}", lineCount, trimmedLine);
                    }
                }
                
                // 출력 읽기 완료
                String output = outputBuilder.get().toString();
                log.debug("FFmpeg 출력 읽기 완료 - 총 {} 줄, {} bytes", lineCount, output.length());
                
                if (output.isEmpty()) {
                    log.warn("FFmpeg 출력이 비어있음 - 프로세스가 출력을 생성하지 않았을 수 있음");
                }
                
                future.complete(output);
                
            } catch (IOException e) {
                log.error("FFmpeg 출력 읽기 실패", e);
                String partialOutput = outputBuilder.get().toString();
                if (!partialOutput.isEmpty()) {
                    log.error("부분 출력: {}", partialOutput.substring(0, Math.min(500, partialOutput.length())));
                }
                future.completeExceptionally(e);
            }
        });
        
        outputThread.setDaemon(true);
        outputThread.setName("FFmpeg-Output-Reader");
        outputThread.start();
        
        return future;
    }

    /**
     * 출력 파일명 생성
     */
    private String generateOutputFileName(String inputFileName, String resolution) {
        int dotIndex = inputFileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return inputFileName + "_" + resolution + "p.mp4";
        }
        String nameWithoutExtension = inputFileName.substring(0, dotIndex);
        String extension = inputFileName.substring(dotIndex);
        return nameWithoutExtension + "_" + resolution + "p" + extension;
    }

    /**
     * FFmpeg 버전 확인 (설치된 FFmpeg 상태 체크)
     */
    public String checkFFmpegVersion() throws IOException {
        String[] command = {"ffmpeg", "-version"};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > 500) break;
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
            return output.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg 버전 확인 중단됨", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 애플리케이션 종료 시 ExecutorService 정리
     */
    @PreDestroy
    public void cleanup() {
        log.info("FFmpegService 종료 - ExecutorService 셧다운 시작");
        encodingExecutor.shutdown();
        try {
            if (!encodingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("ExecutorService가 정상 종료되지 않음 - 강제 종료 시도");
                encodingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("ExecutorService 종료 중 인터럽트 발생", e);
            encodingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("FFmpegService 종료 완료");
    }
}
