package com.egyptian.agent.benchmark;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.egyptian.agent.nlu.EgyptianNormalizer;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.NLUManager;
import com.egyptian.agent.executors.EmergencyHandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Performance Benchmark Test Suite
 * 
 * Measures and validates performance metrics:
 * - End-to-end latency (<2.5s target)
 * - Memory usage (<2GB peak)
 * - Model load time (<30s cold)
 * - ASR real-time factor (<0.5)
 * 
 * Coverage Target: Performance validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Performance Benchmark Tests")
class PerformanceBenchmarkTest {

    @Mock
    private Context mockContext;

    @Mock
    private Context mockApplicationContext;

    private NLUManager nluManager;

    @BeforeEach
    void setUp() {
        // lenient: most benchmark tests never touch the context mock
        lenient().when(mockContext.getApplicationContext()).thenReturn(mockApplicationContext);
    }

    @Nested
    @DisplayName("Latency Benchmark Tests")
    class LatencyBenchmarkTests {

        @Test
        @DisplayName("NLU classification latency under 50ms")
        void testNLUClassificationLatency() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            String input = "كلم ماما";
            int iterations = 100;
            long totalTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                nluManager.classify(input);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            
            long avgLatencyNs = totalTime / iterations;
            long avgLatencyMs = avgLatencyNs / 1_000_000;
            
            assertTrue(avgLatencyMs < 50, 
                "Average NLU latency should be under 50ms, was: " + avgLatencyMs + "ms");
            
            nluManager.destroy();
        }

        @Test
        @DisplayName("Egyptian normalizer latency under 10ms")
        void testEgyptianNormalizerLatency() {
            String input = "كلم ماما دلوقتي";
            int iterations = 1000;
            long totalTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                EgyptianNormalizer.normalize(input);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            
            long avgLatencyNs = totalTime / iterations;
            long avgLatencyMs = avgLatencyNs / 1_000_000;
            
            assertTrue(avgLatencyMs < 10, 
                "Average normalizer latency should be under 10ms, was: " + avgLatencyMs + "ms");
        }

        @Test
        @DisplayName("emergency detection latency under 5ms")
        void testEmergencyDetectionLatency() {
            String input = "نجدة ساعدني";
            int iterations = 1000;
            long totalTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                EmergencyHandler.isEmergency(input);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            
            long avgLatencyNs = totalTime / iterations;
            long avgLatencyMs = avgLatencyNs / 1_000_000;
            
            assertTrue(avgLatencyMs < 5, 
                "Average emergency detection latency should be under 5ms, was: " + avgLatencyMs + "ms");
        }

        @Test
        @DisplayName("contact name normalization latency under 5ms")
        void testContactNameNormalizationLatency() {
            String input = "ماما";
            int iterations = 1000;
            long totalTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                EgyptianNormalizer.normalizeContactName(input);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            
            long avgLatencyNs = totalTime / iterations;
            long avgLatencyMs = avgLatencyNs / 1_000_000;
            
            assertTrue(avgLatencyMs < 5, 
                "Average contact normalization latency should be under 5ms, was: " + avgLatencyMs + "ms");
        }

        @Test
        @DisplayName("time expression normalization latency under 5ms")
        void testTimeExpressionNormalizationLatency() {
            String input = "الصبح";
            int iterations = 1000;
            long totalTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                EgyptianNormalizer.normalizeTimeExpression(input);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            
            long avgLatencyNs = totalTime / iterations;
            long avgLatencyMs = avgLatencyNs / 1_000_000;
            
            assertTrue(avgLatencyMs < 5, 
                "Average time normalization latency should be under 5ms, was: " + avgLatencyMs + "ms");
        }

        @Test
        @DisplayName("end-to-end pipeline latency under 100ms")
        void testEndToEndPipelineLatency() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            String input = "نبهني الصبح";
            int iterations = 100;
            long totalTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                
                // Full pipeline: normalize -> classify
                String normalized = EgyptianNormalizer.normalize(input);
                IntentResult result = nluManager.classify(normalized);
                
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
                
                assertNotNull(result);
            }
            
            long avgLatencyNs = totalTime / iterations;
            long avgLatencyMs = avgLatencyNs / 1_000_000;
            
            assertTrue(avgLatencyMs < 100, 
                "Average end-to-end latency should be under 100ms, was: " + avgLatencyMs + "ms");
            
            nluManager.destroy();
        }
    }

    @Nested
    @DisplayName("Throughput Benchmark Tests")
    class ThroughputBenchmarkTests {

        @Test
        @DisplayName("NLU throughput over 100 requests per second")
        void testNLUThroughput() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            String input = "كلم ماما";
            int targetRPS = 100;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < targetRPS; i++) {
                nluManager.classify(input);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double actualRPS = (targetRPS * 1000.0) / duration;
            
            assertTrue(actualRPS >= 100, 
                "NLU should handle at least 100 RPS, achieved: " + actualRPS);
            
            nluManager.destroy();
        }

        @Test
        @DisplayName("normalizer throughput over 1000 operations per second")
        void testNormalizerThroughput() {
            String input = "كلم ماما دلوقتي";
            int targetOPS = 1000;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < targetOPS; i++) {
                EgyptianNormalizer.normalize(input);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double actualOPS = (targetOPS * 1000.0) / duration;
            
            assertTrue(actualOPS >= 1000, 
                "Normalizer should handle at least 1000 OPS, achieved: " + actualOPS);
        }

        @Test
        @DisplayName("emergency detection throughput over 1000 checks per second")
        void testEmergencyDetectionThroughput() {
            String input = "نجدة";
            int targetCPS = 1000;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < targetCPS; i++) {
                EmergencyHandler.isEmergency(input);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double actualCPS = (targetCPS * 1000.0) / duration;
            
            assertTrue(actualCPS >= 1000, 
                "Emergency detection should handle at least 1000 CPS, achieved: " + actualCPS);
        }

        @Test
        @DisplayName("concurrent NLU requests handling")
        void testConcurrentNLURequests() throws InterruptedException {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            String input = "كلم ماما";
            int numThreads = 10;
            int requestsPerThread = 100;
            Thread[] threads = new Thread[numThreads];
            final int[] successCount = {0};
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < requestsPerThread; j++) {
                        IntentResult result = nluManager.classify(input);
                        if (result != null) {
                            successCount[0]++;
                        }
                    }
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double totalRPS = (successCount[0] * 1000.0) / duration;
            
            assertEquals(numThreads * requestsPerThread, successCount[0]);
            assertTrue(totalRPS >= 50, 
                "Concurrent throughput should be at least 50 RPS, achieved: " + totalRPS);
            
            nluManager.destroy();
        }
    }

    @Nested
    @DisplayName("Memory Benchmark Tests")
    class MemoryBenchmarkTests {

        @Test
        @DisplayName("NLU manager memory footprint under 50MB")
        void testNLUManagerMemoryFootprint() {
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            // Perform some operations
            for (int i = 0; i < 100; i++) {
                nluManager.classify("كلم ماما");
            }
            
            runtime.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = (finalMemory - initialMemory) / (1024 * 1024);
            
            assertTrue(memoryUsed < 50, 
                "NLU manager should use less than 50MB, used: " + memoryUsed + "MB");
            
            nluManager.destroy();
        }

        @Test
        @DisplayName("normalizer memory footprint under 10MB")
        void testNormalizerMemoryFootprint() {
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Perform many normalizations
            for (int i = 0; i < 10000; i++) {
                EgyptianNormalizer.normalize("كلم ماما دلوقتي");
                EgyptianNormalizer.normalizeContactName("ماما");
                EgyptianNormalizer.normalizeTimeExpression("الصبح");
            }
            
            runtime.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = (finalMemory - initialMemory) / (1024 * 1024);
            
            assertTrue(memoryUsed < 10, 
                "Normalizer should use less than 10MB, used: " + memoryUsed + "MB");
        }

        @Test
        @DisplayName("no memory leak in repeated operations")
        void testNoMemoryLeak() {
            Runtime runtime = Runtime.getRuntime();
            
            long[] memoryUsage = new long[5];
            
            for (int round = 0; round < 5; round++) {
                runtime.gc();
                long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
                
                // Perform operations
                for (int i = 0; i < 1000; i++) {
                    nluManager = NLUManager.getInstance(mockContext);
                    nluManager.initialize(false);
                    nluManager.classify("كلم ماما");
                    nluManager.destroy();
                }
                
                runtime.gc();
                long afterMemory = runtime.totalMemory() - runtime.freeMemory();
                memoryUsage[round] = (afterMemory - beforeMemory) / (1024 * 1024);
            }
            
            // Memory usage should not grow significantly
            assertTrue(memoryUsage[4] - memoryUsage[0] < 50, 
                "Memory should not grow more than 50MB over rounds");
        }
    }

    @Nested
    @DisplayName("Scalability Benchmark Tests")
    class ScalabilityBenchmarkTests {

        @Test
        @DisplayName("performance with 1000 consecutive requests")
        void testPerformanceWith1000Requests() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            String input = "كلم ماما";
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 1000; i++) {
                IntentResult result = nluManager.classify(input);
                assertNotNull(result);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double avgLatencyMs = (double) duration / 1000;
            
            assertTrue(avgLatencyMs < 50, 
                "Average latency for 1000 requests should be under 50ms, was: " + avgLatencyMs + "ms");
            
            nluManager.destroy();
        }

        @Test
        @DisplayName("performance degradation under load")
        void testPerformanceDegradationUnderLoad() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            String input = "كلم ماما";
            
            // Measure baseline
            long baselineTime = measureLatency(100, input);
            
            // Measure under load (after many requests)
            for (int i = 0; i < 5000; i++) {
                nluManager.classify(input);
            }
            
            long loadedTime = measureLatency(100, input);
            
            // Performance should not degrade more than 50%
            assertTrue(loadedTime <= baselineTime * 1.5, 
                "Performance should not degrade more than 50% under load");
            
            nluManager.destroy();
        }

        private long measureLatency(int iterations, String input) {
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                nluManager.classify(input);
            }
            long endTime = System.nanoTime();
            return (endTime - startTime) / iterations / 1_000_000; // ms
        }

        @Test
        @DisplayName("intent classification consistency under load")
        void testIntentClassificationConsistency() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            String input = "كلم ماما";
            int iterations = 1000;
            int consistentResults = 0;
            
            for (int i = 0; i < iterations; i++) {
                IntentResult result = nluManager.classify(input);
                if (result.getIntentType().toString().equals("CALL_CONTACT")) {
                    consistentResults++;
                }
            }
            
            double consistencyRate = (double) consistentResults / iterations * 100;
            assertTrue(consistencyRate >= 99.0, 
                "Classification should be 99% consistent, was: " + consistencyRate + "%");
            
            nluManager.destroy();
        }
    }

    @Nested
    @DisplayName("Cold Start Benchmark Tests")
    class ColdStartBenchmarkTests {

        @Test
        @DisplayName("NLU manager initialization under 5 seconds")
        void testNLUManagerInitialization() {
            long startTime = System.currentTimeMillis();
            
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            // Wait for initialization to complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            assertTrue(duration < 5000, 
                "NLU manager initialization should complete in under 5 seconds, took: " + duration + "ms");
            
            nluManager.destroy();
        }

        @Test
        @DisplayName("normalizer static initialization under 1 second")
        void testNormalizerStaticInitialization() {
            long startTime = System.currentTimeMillis();
            
            // Force class loading and static initialization
            EgyptianNormalizer.normalize("test");
            
            long duration = System.currentTimeMillis() - startTime;
            
            assertTrue(duration < 1000, 
                "Normalizer static initialization should complete in under 1 second, took: " + duration + "ms");
        }

        @Test
        @DisplayName("first classification latency under 200ms")
        void testFirstClassificationLatency() {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            // First classification (cold)
            long startTime = System.nanoTime();
            IntentResult result = nluManager.classify("كلم ماما");
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            
            assertNotNull(result);
            assertTrue(duration < 200, 
                "First classification should complete in under 200ms, took: " + duration + "ms");
            
            nluManager.destroy();
        }
    }

    @Nested
    @DisplayName("Real-Time Factor Benchmark Tests")
    class RealTimeFactorBenchmarkTests {

        @Test
        @DisplayName("processing faster than real-time speech")
        void testProcessingFasterThanRealTime() {
            // Simulate processing 1 second of speech (approximately 16000 samples)
            // Processing should complete in less than 500ms (RTF < 0.5)
            
            String speechEquivalent = "كلم ماما دلوقتي حالا";
            int iterations = 100;
            long totalTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                
                EgyptianNormalizer.normalize(speechEquivalent);
                nluManager = NLUManager.getInstance(mockContext);
                nluManager.initialize(false);
                nluManager.classify(speechEquivalent);
                nluManager.destroy();
                
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            
            double avgProcessingTimeMs = (double) totalTime / iterations / 1_000_000;
            double realTimeFactor = avgProcessingTimeMs / 1000; // Assuming 1 second of speech
            
            assertTrue(realTimeFactor < 0.5, 
                "Real-time factor should be under 0.5, was: " + realTimeFactor);
        }
    }

    @Nested
    @DisplayName("Stress Benchmark Tests")
    class StressBenchmarkTests {

        @Test
        @DisplayName("sustained high load for 30 seconds")
        void testSustainedHighLoad() throws InterruptedException {
            nluManager = NLUManager.getInstance(mockContext);
            nluManager.initialize(false);
            
            String input = "كلم ماما";
            long duration = 30000; // 30 seconds
            long startTime = System.currentTimeMillis();
            int requestCount = 0;
            
            while (System.currentTimeMillis() - startTime < duration) {
                nluManager.classify(input);
                requestCount++;
            }
            
            double rps = (requestCount * 1000.0) / duration;
            
            assertTrue(rps >= 50, 
                "Should sustain at least 50 RPS for 30 seconds, achieved: " + rps);
            
            nluManager.destroy();
        }

        @Test
        @DisplayName("rapid initialization and destruction")
        void testRapidInitializationDestruction() {
            int iterations = 100;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < iterations; i++) {
                NLUManager manager = NLUManager.getInstance(mockContext);
                manager.initialize(false);
                manager.classify("كلم ماما");
                manager.destroy();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double avgTimePerCycle = (double) duration / iterations;
            
            assertTrue(avgTimePerCycle < 100, 
                "Average init/destroy cycle should be under 100ms, was: " + avgTimePerCycle + "ms");
        }
    }
}
