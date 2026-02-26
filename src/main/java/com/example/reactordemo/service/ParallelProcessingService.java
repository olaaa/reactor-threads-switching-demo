// src/main/java/com/example/reactordemo/service/ParallelProcessingService.java
package com.example.reactordemo.service;

import com.example.reactordemo.model.ImageData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для демонстрации параллельной обработки данных в Project Reactor 3.8.3.
 * 
 * <p>Основная цель: продемонстрировать разницу между:</p>
 * <ul>
 *   <li>Последовательной обработкой (обычный Flux)</li>
 *   <li>.parallel() БЕЗ .runOn() - разделение на рельсы, но на одном потоке</li>
 *   <li>.parallel().runOn(Schedulers.parallel()) - настоящий параллелизм</li>
 * </ul>
 * 
 * <p>Каждый метод содержит ДЕТАЛЬНОЕ логирование для визуализации работы потоков.</p>
 * 
 * <p><b>Ключевая концепция:</b> {@code .parallel()} сам по себе НЕ создает параллелизма!
 * Он только разделяет Flux на "рельсы" (rails). Для настоящего параллелизма
 * необходимо добавить {@code .runOn(Schedulers.parallel())}.</p>
 * 
 * @see <a href="https://projectreactor.io/docs/core/release/reference/#advanced-parallelizing-parall">Reactor Documentation: ParallelFlux</a>
 */
@Service
@Slf4j
public class ParallelProcessingService {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    /**
     * МЕТОД 1: Последовательная обработка (baseline для сравнения).
     * 
     * <p>Обрабатывает элементы один за другим на том потоке, который вызвал подписку.
     * Это самый простой подход, но самый медленный для CPU-bound операций.</p>
     * 
     * <p><b>Ожидаемый результат:</b></p>
     * <ul>
     *   <li>Все элементы обрабатываются на ОДНОМ потоке</li>
     *   <li>Обработка занимает: 4 элемента * 500мс = ~2000мс</li>
     *   <li>Имя потока: обычно "Test worker" или "main"</li>
     * </ul>
     * 
     * @param images поток изображений для обработки
     * @return Flux обработанных изображений
     */
    public Flux<ImageData> processSequentially(Flux<ImageData> images) {
        log.info("========================================");
        log.info("ПОСЛЕДОВАТЕЛЬНАЯ ОБРАБОТКА (baseline)");
        log.info("========================================");
        
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger counter = new AtomicInteger(0);
        
        return images
            .doOnNext(img -> {
                String threadName = Thread.currentThread().getName();
                String time = LocalTime.now().format(TIME_FORMATTER);
                int index = counter.incrementAndGet();
                
                log.info("[SEQUENTIAL] [START] Element #{}: {} | Thread: {} | Time: {}", 
                    index, img.filename(), threadName, time);
                usedThreads.add(threadName);
            })
            .map(img -> {
                String threadName = Thread.currentThread().getName();
                String time = LocalTime.now().format(TIME_FORMATTER);
                
                log.info("[SEQUENTIAL] [PROCESSING] Element: {} | Thread: {} | Time: {}", 
                    img.filename(), threadName, time);
                
                ImageData processed = img.processImage(); // 500ms
                
                log.info("[SEQUENTIAL] [COMPLETE] Element: {} | Thread: {} | Time: {}", 
                    processed.filename(), threadName, LocalTime.now().format(TIME_FORMATTER));
                
                return processed;
            })
            .doOnComplete(() -> {
                log.info("[SEQUENTIAL] [SUMMARY] Обработка завершена!");
                log.info("[SEQUENTIAL] [SUMMARY] Использовано потоков: {}", usedThreads.size());
                log.info("[SEQUENTIAL] [SUMMARY] Имена потоков: {}", usedThreads);
                log.info("[SEQUENTIAL] [SUMMARY] Результат: ❌ НЕТ ПАРАЛЛЕЛИЗМА (все на одном потоке)");
                log.info("========================================\n");
            });
    }
    
    /**
     * МЕТОД 2: Использование .parallel() БЕЗ .runOn().
     * 
     * <p>Этот метод демонстрирует ВАЖНУЮ концепцию: {@code .parallel()} сам по себе
     * НЕ создает параллелизма! Он только разделяет поток на "рельсы" (rails).</p>
     * 
     * <p><b>Что происходит:</b></p>
     * <ol>
     *   <li>Flux разделяется на N рельсов (N = количество процессоров)</li>
     *   <li>Каждый элемент попадает на свой рельс (round-robin)</li>
     *   <li>НО: все рельсы выполняются на ОДНОМ потоке</li>
     *   <li>Никакого выигрыша по времени нет!</li>
     * </ol>
     * 
     * <p><b>Ожидаемый результат:</b></p>
     * <ul>
     *   <li>Элементы распределены по 4 рельсам (0, 1, 2, 3)</li>
     *   <li>Но все на ОДНОМ потоке (Test worker)</li>
     *   <li>Время выполнения: ~2000мс (как и в последовательном)</li>
     * </ul>
     * 
     * @param images поток изображений для обработки
     * @return Flux обработанных изображений
     */
    public Flux<ImageData> processWithParallelOnly(Flux<ImageData> images) {
        log.info("========================================");
        log.info(".parallel() БЕЗ .runOn()");
        log.info("========================================");
        
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger counter = new AtomicInteger(0);
        
        return images
            .parallel(4) // Разделение на 4 рельса
            .doOnNext(img -> {
                String threadName = Thread.currentThread().getName();
                String time = LocalTime.now().format(TIME_FORMATTER);
                int index = counter.getAndIncrement();
                int railNumber = index % 4;
                
                log.info("[PARALLEL-ONLY] [START] Element #{}: {} | Rail: {} | Thread: {} | Time: {}", 
                    index, img.filename(), railNumber, threadName, time);
                usedThreads.add(threadName);
            })
            .map(img -> {
                String threadName = Thread.currentThread().getName();
                String time = LocalTime.now().format(TIME_FORMATTER);
                
                log.info("[PARALLEL-ONLY] [PROCESSING] Element: {} | Thread: {} | Time: {}", 
                    img.filename(), threadName, time);
                
                ImageData processed = img.processImage(); // 500ms
                
                log.info("[PARALLEL-ONLY] [COMPLETE] Element: {} | Thread: {} | Time: {}", 
                    processed.filename(), threadName, LocalTime.now().format(TIME_FORMATTER));
                
                return processed;
            })
            .sequential() // Обратно в обычный Flux
            .doOnComplete(() -> {
                log.info("[PARALLEL-ONLY] [SUMMARY] Обработка завершена!");
                log.info("[PARALLEL-ONLY] [SUMMARY] Использовано потоков: {}", usedThreads.size());
                log.info("[PARALLEL-ONLY] [SUMMARY] Имена потоков: {}", usedThreads);
                log.info("[PARALLEL-ONLY] [SUMMARY] Результат: ❌ НЕТ ПАРАЛЛЕЛИЗМА!");
                log.info("[PARALLEL-ONLY] [SUMMARY] Объяснение: .parallel() разделил на рельсы, но без .runOn() все на одном потоке!");
                log.info("========================================\n");
            });
    }
    
    /**
     * МЕТОД 3: Использование .parallel().runOn(Schedulers.parallel()) - НАСТОЯЩИЙ ПАРАЛЛЕЛИЗМ!
     * 
     * <p>Этот метод демонстрирует ПРАВИЛЬНОЕ использование параллелизма в Reactor.</p>
     * 
     * <p><b>Что происходит:</b></p>
     * <ol>
     *   <li>{@code .parallel(4)} - разделяет поток на 4 рельса</li>
     *   <li>{@code .runOn(Schedulers.parallel())} - назначает каждому рельсу СВОЙ поток</li>
     *   <li>Каждый рельс выполняется на своем потоке из пула parallel</li>
     *   <li>Элементы обрабатываются ОДНОВРЕМЕННО на разных потоках</li>
     * </ol>
     * 
     * <p><b>Ожидаемый результат:</b></p>
     * <ul>
     *   <li>4 элемента обрабатываются на 4 РАЗНЫХ потоках</li>
     *   <li>Имена потоков: parallel-1, parallel-2, parallel-3, parallel-4</li>
     *   <li>Время выполнения: ~500мс (в 4 раза быстрее!)</li>
     *   <li>Использовано 4 уникальных потока</li>
     * </ul>
     * 
     * <p><b>Когда использовать:</b></p>
     * <ul>
     *   <li>CPU-bound операции (обработка изображений, хеширование, вычисления)</li>
     *   <li>Тяжелые синхронные операции</li>
     *   <li>Когда нужно максимально использовать процессоры</li>
     * </ul>
     * 
     * <p><b>Когда НЕ использовать:</b></p>
     * <ul>
     *   <li>I/O операции (лучше использовать .flatMap с concurrency)</li>
     *   <li>Операции с shared state (может вызвать race conditions)</li>
     *   <li>Маленькие потоки данных (overhead больше выигрыша)</li>
     * </ul>
     * 
     * @param images поток изображений для обработки
     * @return Flux обработанных изображений
     */
    public Flux<ImageData> processWithParallelAndRunOn(Flux<ImageData> images) {
        log.info("========================================");
        log.info(".parallel().runOn(Schedulers.parallel()) - НАСТОЯЩИЙ ПАРАЛЛЕЛИЗМ!");
        log.info("========================================");
        
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger counter = new AtomicInteger(0);
        
        return images
            .parallel(4) // Разделение на 4 рельса
            .runOn(Schedulers.parallel()) // ✅ Каждый рельс на своем потоке!
            .doOnNext(img -> {
                String threadName = Thread.currentThread().getName();
                String time = LocalTime.now().format(TIME_FORMATTER);
                int index = counter.getAndIncrement();
                int railNumber = index % 4;
                
                log.info("[PARALLEL-RUNON] [START] Element #{}: {} | Rail: {} | Thread: {} | Time: {}", 
                    index, img.filename(), railNumber, threadName, time);
                usedThreads.add(threadName);
            })
            .map(img -> {
                String threadName = Thread.currentThread().getName();
                String time = LocalTime.now().format(TIME_FORMATTER);
                
                log.info("[PARALLEL-RUNON] [PROCESSING] Element: {} | Thread: {} | Time: {}", 
                    img.filename(), threadName, time);
                
                ImageData processed = img.processImage(); // 500ms
                
                log.info("[PARALLEL-RUNON] [COMPLETE] Element: {} | Thread: {} | Time: {}", 
                    processed.filename(), threadName, LocalTime.now().format(TIME_FORMATTER));
                
                return processed;
            })
            .sequential() // Обратно в обычный Flux
            .doOnComplete(() -> {
                log.info("[PARALLEL-RUNON] [SUMMARY] Обработка завершена!");
                log.info("[PARALLEL-RUNON] [SUMMARY] Использовано потоков: {}", usedThreads.size());
                log.info("[PARALLEL-RUNON] [SUMMARY] Имена потоков: {}", usedThreads);
                log.info("[PARALLEL-RUNON] [SUMMARY] Результат: ✅ НАСТОЯЩИЙ ПАРАЛЛЕЛИЗМ!");
                log.info("[PARALLEL-RUNON] [SUMMARY] Объяснение: каждый рельс выполняется на своем потоке!");
                log.info("========================================\n");
            });
    }
    
    /**
     * ДОПОЛНИТЕЛЬНЫЙ МЕТОД: Обработка большего набора данных для измерения производительности.
     * 
     * <p>Создает и обрабатывает указанное количество изображений параллельно,
     * собирая статистику использования потоков.</p>
     * 
     * @param itemCount количество элементов для обработки
     * @return Flux обработанных изображений с детальной статистикой
     */
    public Flux<ImageData> processLargeDataset(int itemCount) {
        log.info("========================================");
        log.info("ОБРАБОТКА БОЛЬШОГО НАБОРА ДАННЫХ ({} элементов)", itemCount);
        log.info("========================================");
        
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger processedCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        return Flux.range(1, itemCount)
            .map(ImageData::createTestImage)
            .parallel()
            .runOn(Schedulers.parallel())
            .doOnNext(img -> usedThreads.add(Thread.currentThread().getName()))
            .map(ImageData::processImage)
            .doOnNext(img -> {
                int count = processedCount.incrementAndGet();
                if (count % 10 == 0) {
                    log.info("[LARGE-DATASET] Обработано {} из {} элементов...", count, itemCount);
                }
            })
            .sequential()
            .doOnComplete(() -> {
                long totalTime = System.currentTimeMillis() - startTime;
                double avgTime = (double) totalTime / itemCount;
                
                log.info("[LARGE-DATASET] [SUMMARY] Обработка завершена!");
                log.info("[LARGE-DATASET] [SUMMARY] Всего элементов: {}", itemCount);
                log.info("[LARGE-DATASET] [SUMMARY] Общее время: {} мс", totalTime);
                log.info("[LARGE-DATASET] [SUMMARY] Среднее время на элемент: {} мс", String.format("%.2f", avgTime));                log.info("[LARGE-DATASET] [SUMMARY] Использовано потоков: {}", usedThreads.size());
                log.info("[LARGE-DATASET] [SUMMARY] Имена потоков: {}", usedThreads);
                log.info("========================================\n");
            });
    }
}
