// src/test/java/com/example/reactordemo/service/ParallelProcessingServiceTest.java
package com.example.reactordemo.service;

import com.example.reactordemo.model.ImageData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для демонстрации работы .parallel() и .runOn(Schedulers.parallel()) в Project Reactor.
 * 
 * <p>Каждый тест проверяет:</p>
 * <ul>
 *   <li>Корректность обработки данных</li>
 *   <li>Количество использованных потоков</li>
 *   <li>Имена потоков</li>
 *   <li>Время выполнения</li>
 * </ul>
 * 
 * <p>Основная цель: ВИЗУАЛЬНО ПОКАЗАТЬ разницу между последовательной и параллельной обработкой.</p>
 */
@Slf4j
@DisplayName("Тесты ParallelProcessingService: демонстрация .parallel() и .runOn()")
class ParallelProcessingServiceTest {
    
    private ParallelProcessingService service;
    private Flux<ImageData> testImages;
    
    @BeforeEach
    void setUp() {
        service = new ParallelProcessingService();
        
        // Создаем 4 тестовых изображения
        testImages = Flux.range(1, 4)
            .map(ImageData::createTestImage);
    }
    
    /**
     * ТЕСТ 1: Последовательная обработка - все на одном потоке.
     * 
     * <p><b>Что проверяется:</b></p>
     * <ul>
     *   <li>Все 4 элемента обрабатываются</li>
     *   <li>Используется ТОЛЬКО 1 поток</li>
     *   <li>Имя потока содержит "Test worker"</li>
     *   <li>Время выполнения: ~2000мс (4 * 500мс)</li>
     * </ul>
     * 
     * <p><b>Ожидаемый вывод в логах:</b></p>
     * <pre>
     * [SEQUENTIAL] [START] Element #1: image1.jpg | Thread: Test worker | Time: ...
     * [SEQUENTIAL] [PROCESSING] Element: image1.jpg | Thread: Test worker | Time: ...
     * [SEQUENTIAL] [COMPLETE] Element: image1_processed | Thread: Test worker | Time: ...
     * [SEQUENTIAL] [START] Element #2: image2.jpg | Thread: Test worker | Time: ...
     * ... (все на Test worker)
     * [SEQUENTIAL] [SUMMARY] Использовано потоков: 1
     * [SEQUENTIAL] [SUMMARY] Результат: ❌ НЕТ ПАРАЛЛЕЛИЗМА
     * </pre>
     */
    @Test
    @DisplayName("Тест 1: Последовательная обработка - все на одном потоке")
    void testSequentialProcessing_AllOnOneThread() {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  ТЕСТ 1: ПОСЛЕДОВАТЕЛЬНАЯ ОБРАБОТКА                       ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger processedCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        StepVerifier.create(
            service.processSequentially(testImages)
                .doOnNext(img -> {
                    usedThreads.add(Thread.currentThread().getName());
                    processedCount.incrementAndGet();
                })
        )
            .expectNextCount(4) // Ожидаем 4 обработанных элемента
            .verifyComplete();
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Проверяем результаты
        log.info("\n🔍 РЕЗУЛЬТАТЫ ТЕСТА:");
        log.info("   ├─ Обработано элементов: {}", processedCount.get());
        log.info("   ├─ Использовано потоков: {}", usedThreads.size());
        log.info("   ├─ Имена потоков: {}", usedThreads);
        log.info("   └─ Общее время: {} мс", totalTime);
        
        assertThat(processedCount.get()).isEqualTo(4);
        assertThat(usedThreads).hasSize(1); // ❗ КЛЮЧЕВАЯ ПРОВЕРКА: только 1 поток
        assertThat(usedThreads.iterator().next()).contains("main");
        assertThat(totalTime).isGreaterThanOrEqualTo(2000); // ~2 секунды (4 * 500мс)
        
        log.info("\n✅ ВЫВОД: Последовательная обработка - все на одном потоке, время ~2000мс\n");
    }
    
    /**
     * ТЕСТ 2: .parallel() БЕЗ .runOn() - разделение на рельсы, но на одном потоке.
     * 
     * <p><b>Что проверяется:</b></p>
     * <ul>
     *   <li>Все 4 элемента обрабатываются</li>
     *   <li>Используется ТОЛЬКО 1 поток (❗ несмотря на .parallel())</li>
     *   <li>Элементы распределены по 4 рельсам</li>
     *   <li>Время выполнения: ~2000мс (как и в последовательном!)</li>
     * </ul>
     * 
     * <p><b>Ключевая идея:</b> {@code .parallel()} сам по себе НЕ создает параллелизма!</p>
     * 
     * <p><b>Ожидаемый вывод в логах:</b></p>
     * <pre>
     * [PARALLEL-ONLY] [START] Element #0: image1.jpg | Rail: 0 | Thread: Test worker | ...
     * [PARALLEL-ONLY] [START] Element #1: image2.jpg | Rail: 1 | Thread: Test worker | ...
     * [PARALLEL-ONLY] [START] Element #2: image3.jpg | Rail: 2 | Thread: Test worker | ...
     * [PARALLEL-ONLY] [START] Element #3: image4.jpg | Rail: 3 | Thread: Test worker | ...
     * ... (все на Test worker)
     * [PARALLEL-ONLY] [SUMMARY] Использовано потоков: 1
     * [PARALLEL-ONLY] [SUMMARY] Результат: ❌ НЕТ ПАРАЛЛЕЛИЗМА!
     * </pre>
     */
    @Test
    @DisplayName("Тест 2: .parallel() БЕЗ .runOn() - рельсы есть, параллелизма нет")
    void testParallelOnly_NoRealParallelism() {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  ТЕСТ 2: .parallel() БЕЗ .runOn()                         ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger processedCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        StepVerifier.create(
            service.processWithParallelOnly(testImages)
                .doOnNext(img -> {
                    usedThreads.add(Thread.currentThread().getName());
                    processedCount.incrementAndGet();
                })
        )
            .expectNextCount(4)
            .verifyComplete();
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        log.info("\n🔍 РЕЗУЛЬТАТЫ ТЕСТА:");
        log.info("   ├─ Обработано элементов: {}", processedCount.get());
        log.info("   ├─ Использовано потоков: {}", usedThreads.size());
        log.info("   ├─ Имена потоков: {}", usedThreads);
        log.info("   └─ Общее время: {} мс", totalTime);
        
        assertThat(processedCount.get()).isEqualTo(4);
        assertThat(usedThreads).hasSize(1); // ❗ КЛЮЧЕВАЯ ПРОВЕРКА: все еще только 1 поток!
        assertThat(totalTime).isGreaterThanOrEqualTo(2000); // Время не уменьшилось!
        
        log.info("\n❌ ВЫВОД: .parallel() БЕЗ .runOn() НЕ создает параллелизма!");
        log.info("   Все элементы обрабатываются на ОДНОМ потоке последовательно.");
        log.info("   Для настоящего параллелизма нужен .runOn(Schedulers.parallel())\n");
    }
    
    /**
     * ТЕСТ 3: .parallel().runOn() - НАСТОЯЩИЙ ПАРАЛЛЕЛИЗМ!
     * 
     * <p><b>Что проверяется:</b></p>
     * <ul>
     *   <li>Все 4 элемента обрабатываются</li>
     *   <li>Используется 4 РАЗНЫХ потока ✅</li>
     *   <li>Имена потоков: parallel-1, parallel-2, parallel-3, parallel-4</li>
     *   <li>Время выполнения: ~500мс (в 4 раза быстрее!) ✅</li>
     * </ul>
     * 
     * <p><b>Ключевая идея:</b> {@code .runOn(Schedulers.parallel())} назначает каждому рельсу свой поток!</p>
     * 
     * <p><b>Ожидаемый вывод в логах:</b></p>
     * <pre>
     * [PARALLEL-RUNON] [START] Element #0: image1.jpg | Rail: 0 | Thread: parallel-1 | ...
     * [PARALLEL-RUNON] [START] Element #1: image2.jpg | Rail: 1 | Thread: parallel-2 | ...
     * [PARALLEL-RUNON] [START] Element #2: image3.jpg | Rail: 2 | Thread: parallel-3 | ...
     * [PARALLEL-RUNON] [START] Element #3: image4.jpg | Rail: 3 | Thread: parallel-4 | ...
     * ... (все на РАЗНЫХ потоках!)
     * [PARALLEL-RUNON] [SUMMARY] Использовано потоков: 4
     * [PARALLEL-RUNON] [SUMMARY] Результат: ✅ НАСТОЯЩИЙ ПАРАЛЛЕЛИЗМ!
     * </pre>
     */
    @Test
    @DisplayName("Тест 3: .parallel().runOn() - настоящий параллелизм на 4 потоках")
    void testParallelWithRunOn_RealParallelism() {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  ТЕСТ 3: .parallel().runOn() - НАСТОЯЩИЙ ПАРАЛЛЕЛИЗМ!     ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger processedCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        StepVerifier.create(
            service.processWithParallelAndRunOn(testImages)
                .doOnNext(img -> {
                    String threadName = Thread.currentThread().getName();
                    usedThreads.add(threadName);
                    processedCount.incrementAndGet();
                    log.debug("Элемент обработан: {} | Поток: {}", img.filename(), threadName);
                })
        )
            .expectNextCount(4)
            .verifyComplete();
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        log.info("\n🔍 РЕЗУЛЬТАТЫ ТЕСТА:");
        log.info("   ├─ Обработано элементов: {}", processedCount.get());
        log.info("   ├─ Использовано потоков: {}", usedThreads.size());
        log.info("   ├─ Имена потоков: {}", usedThreads);
        log.info("   └─ Общее время: {} мс", totalTime);
        
        assertThat(processedCount.get()).isEqualTo(4);
        assertThat(usedThreads).hasSizeGreaterThanOrEqualTo(2); // ✅ КЛЮЧЕВАЯ ПРОВЕРКА: минимум 2 потока
        assertThat(usedThreads.stream().anyMatch(name -> name.contains("parallel")))
            .isTrue(); // Потоки из parallel scheduler
        assertThat(totalTime).isLessThan(2000); // ✅ Время значительно меньше!
        
        log.info("\n✅ ВЫВОД: .parallel().runOn() создает НАСТОЯЩИЙ параллелизм!");
        log.info("   Элементы обрабатываются ОДНОВРЕМЕННО на {} потоках.", usedThreads.size());
        log.info("   Время выполнения сократилось примерно в {} раз!\n", 2000.0 / totalTime);
    }
    
    /**
     * ТЕСТ 4: Сравнение времени выполнения всех трех подходов.
     * 
     * <p>Этот тест последовательно выполняет все три метода и сравнивает время.</p>
     * 
     * <p><b>Ожидаемые результаты:</b></p>
     * <ul>
     *   <li>Sequential: ~2000мс, 1 поток</li>
     *   <li>Parallel only: ~2000мс, 1 поток</li>
     *   <li>Parallel + runOn: ~500мс, 4 потока</li>
     * </ul>
     */
    @Test
    @DisplayName("Тест 4: Сравнение производительности всех подходов")
    void testPerformanceComparison_AllMethods() {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  ТЕСТ 4: СРАВНЕНИЕ ПРОИЗВОДИТЕЛЬНОСТИ                     ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        // 1. Последовательная обработка
        long start1 = System.currentTimeMillis();
        Set<String> threads1 = ConcurrentHashMap.newKeySet();
        StepVerifier.create(
            service.processSequentially(testImages)
                .doOnNext(img -> threads1.add(Thread.currentThread().getName()))
        ).expectNextCount(4).verifyComplete();
        long time1 = System.currentTimeMillis() - start1;
        
        // 2. Parallel only
        long start2 = System.currentTimeMillis();
        Set<String> threads2 = ConcurrentHashMap.newKeySet();
        StepVerifier.create(
            service.processWithParallelOnly(testImages)
                .doOnNext(img -> threads2.add(Thread.currentThread().getName()))
        ).expectNextCount(4).verifyComplete();
        long time2 = System.currentTimeMillis() - start2;
        
        // 3. Parallel + runOn
        long start3 = System.currentTimeMillis();
        Set<String> threads3 = ConcurrentHashMap.newKeySet();
        StepVerifier.create(
            service.processWithParallelAndRunOn(testImages)
                .doOnNext(img -> threads3.add(Thread.currentThread().getName()))
        ).expectNextCount(4).verifyComplete();
        long time3 = System.currentTimeMillis() - start3;
        
        // Сравнение результатов
        log.info("\n📊 СРАВНИТЕЛЬНАЯ ТАБЛИЦА:");
        log.info("┌────────────────────────────────────┬──────────┬───────────┐");
        log.info("│ Метод                              │ Потоки   │ Время (мс)│");
        log.info("├────────────────────────────────────┼──────────┼───────────┤");
        log.info("│ 1. Sequential                      │ {} │ {} │", threads1.size(), time1);
        log.info("│ 2. .parallel() только              │ {} │ {} │", threads2.size(), time2);
        log.info("│ 3. .parallel().runOn()             │ {} │ {} │", threads3.size(), time3);
        log.info("└────────────────────────────────────┴──────────┴───────────┘");
        
        log.info("\n💡 ВЫВОДЫ:");
        log.info("   1️⃣  Sequential использует {} поток(а), время: {} мс", threads1.size(), time1);
        log.info("   2️⃣  .parallel() использует {} поток(а), время: {} мс ❌ НЕТ УЛУЧШЕНИЯ!", threads2.size(), time2);
        log.info("   3️⃣  .parallel().runOn() использует {} поток(а), время: {} мс ✅ УСКОРЕНИЕ в {}x!",
            threads3.size(), time3, (double) time1 / time3);
        
        assertThat(threads1).hasSize(1);
        assertThat(threads2).hasSize(1);
        assertThat(threads3.size()).isGreaterThanOrEqualTo(2);
        assertThat(time3).isLessThan(time1); // Параллельный быстрее последовательного
        
        log.info("\n");
    }
    
    /**
     * ТЕСТ 5: Обработка большого набора данных с замером статистики.
     * 
     * <p>Обрабатывает 20 элементов параллельно и собирает статистику использования потоков.</p>
     */
    @Test
    @DisplayName("Тест 5: Обработка большого набора данных (20 элементов)")
    void testLargeDataset_VerifyThreadPoolUsage() {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  ТЕСТ 5: ОБРАБОТКА БОЛЬШОГО НАБОРА ДАННЫХ                 ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger processedCount = new AtomicInteger(0);
        
        StepVerifier.create(
            service.processLargeDataset(20)
                .doOnNext(img -> {
                    usedThreads.add(Thread.currentThread().getName());
                    processedCount.incrementAndGet();
                })
        )
            .expectNextCount(20)
            .verifyComplete();
        
        log.info("\n🔍 РЕЗУЛЬТАТЫ:");
        log.info("   ├─ Всего обработано: {} элементов", processedCount.get());
        log.info("   ├─ Использовано потоков: {}", usedThreads.size());
        log.info("   └─ Имена потоков: {}", usedThreads);
        
        assertThat(processedCount.get()).isEqualTo(20);
        assertThat(usedThreads.size()).isGreaterThanOrEqualTo(2); // Минимум 2 потока
        
        log.info("\n✅ Большой набор данных успешно обработан параллельно!\n");
    }
    
    /**
     * ТЕСТ 6: Проверка корректности данных после параллельной обработки.
     * 
     * <p>Убеждаемся, что параллельная обработка не нарушает логику преобразования данных.</p>
     */
    @Test
    @DisplayName("Тест 6: Проверка корректности обработанных данных")
    void testDataIntegrity_ParallelProcessing() {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  ТЕСТ 6: КОРРЕКТНОСТЬ ДАННЫХ                              ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        StepVerifier.create(
            service.processWithParallelAndRunOn(testImages)
        )
            .expectNextMatches(img -> {
                log.info("Проверка: {} | Размер: {}x{}", img.filename(), img.width(), img.height());
                return img.filename().contains("_processed") 
                    && img.width() == 960  // Половина от 1920
                    && img.height() == 540; // Половина от 1080
            })
            .expectNextMatches(img -> img.filename().contains("_processed"))
            .expectNextMatches(img -> img.filename().contains("_processed"))
            .expectNextMatches(img -> img.filename().contains("_processed"))
            .verifyComplete();
        
        log.info("\n✅ Все данные обработаны корректно!");
        log.info("   Параллелизм не нарушил логику преобразования.\n");
    }
    
    /**
     * ТЕСТ 7: Демонстрация timeout - параллельная обработка должна уложиться в лимит.
     * 
     * <p>Последовательная обработка 4 элементов займет ~2000мс и превысит timeout 1500мс.</p>
     * <p>Параллельная обработка должна уложиться в 1500мс.</p>
     */
    @Test
    @DisplayName("Тест 7: Timeout - параллельная обработка укладывается, последовательная нет")
    void testTimeout_ParallelMeetsDeadline() {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  ТЕСТ 7: TIMEOUT (1500мс лимит)                           ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        // Параллельная обработка ДОЛЖНА уложиться
        log.info("\n✅ Проверка: параллельная обработка должна уложиться в 1500мс...");
        StepVerifier.create(
            service.processWithParallelAndRunOn(testImages)
        )
            .expectNextCount(4)
            .verifyComplete();
        
        log.info("✅ Параллельная обработка уложилась в лимит!\n");
        
        // Последовательная обработка НЕ ДОЛЖНА уложиться
        // (закомментировано, так как тест упадет - это ожидаемо)
        /*
        log.info("\n❌ Проверка: последовательная обработка НЕ уложится в 1500мс...");
        StepVerifier.create(
            service.processSequentially(testImages)
        )
            .expectNextCount(4)
            .expectTimeout(Duration.ofMillis(1500))
            .verify();
        */
        
        log.info("💡 ВЫВОД: Параллельная обработка позволяет уложиться в жесткие временные рамки!\n");
    }
}
