// src/main/java/com/example/reactordemo/model/ImageData.java
package com.example.reactordemo.model;

/**
 * Record класс для демонстрации параллельной обработки изображений в Project Reactor.
 * 
 * Представляет собой данные об изображении, которое нужно обработать.
 * Используется для демонстрации CPU-bound операций (обработка изображений),
 * которые выигрывают от параллелизма.
 * 
 * @param filename имя файла изображения
 * @param width ширина изображения в пикселях
 * @param height высота изображения в пикселях
 * @param format формат изображения (jpg, png, webp и т.д.)
 */
public record ImageData(
    String filename,
    int width,
    int height,
    String format
) {
    
    /**
     * Имитирует тяжелую CPU-bound операцию обработки изображения.
     * 
     * В реальном приложении это могло бы быть:
     * - Изменение размера (resize)
     * - Применение фильтров (blur, sharpen, color correction)
     * - Конвертация формата (PNG → WebP, JPG → AVIF)
     * - Сжатие с потерями/без потерь
     * 
     * Для демонстрации используется Thread.sleep(500) для имитации
     * долгой операции (~500мс на обработку одного изображения).
     * 
     * @return новый объект ImageData с обработанными параметрами
     */
    public ImageData processImage() {
        try {
            // Имитация тяжелой операции обработки изображения
            // В реальности это могло бы быть:
            // - ImageIO.read() + resize + filters + ImageIO.write()
            // - FFmpeg вызов для конвертации видео
            // - Применение ML модели для улучшения качества
            Thread.sleep(500); // 500ms на обработку
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Обработка изображения прервана", e);
        }
        
        // Возвращаем "обработанное" изображение с уменьшенным разрешением
        return new ImageData(
            filename + "_processed",
            width / 2,
            height / 2,
            format
        );
    }
    
    /**
     * Удобный метод для создания тестовых данных.
     * 
     * @param index порядковый номер изображения
     * @return объект ImageData с тестовыми данными
     */
    public static ImageData createTestImage(int index) {
        return new ImageData(
            String.format("image%d.jpg", index),
            1920,
            1080,
            "jpg"
        );
    }
}
