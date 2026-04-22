package com.atguigu.tingshu.common.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * 计算图片的平均图像电平 (APL, Average Picture Level)
 * 基于 sRGB 色彩空间，考虑伽马校正，将非线性 RGB 值转换为线性光亮度后求平均。
 */
public class ImageAPLCalculator {

    public static void main(String[] args) {
        String imagePath = getImagePath(args);
        if (imagePath == null || imagePath.isEmpty()) {
            System.err.println("未提供有效的图片路径。");
            return;
        }

        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            if (image == null) {
                System.err.println("无法读取图片文件，请检查路径或文件格式。");
                return;
            }

            double apl = calculateAPL(image);
            System.out.printf("图片 APL (线性亮度平均值) = %.4f (%.2f%%)%n", apl, apl * 100);
        } catch (IOException e) {
            System.err.println("读取图片时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取图片路径（优先命令行参数，否则从控制台读取）
     */
    private static String getImagePath(String[] args) {
        if (args.length > 0) {
            return args[0];
        }
        System.out.print("请输入图片文件路径: ");
        try (Scanner scanner = new Scanner(System.in)) {
            return scanner.nextLine().trim();
        }
    }

    /**
     * 计算整个图片的平均亮度 (APL)
     * @param image BufferedImage 对象
     * @return 线性亮度的平均值，范围 [0, 1]
     */
    public static double calculateAPL(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long pixelCount = (long) width * height;
        double totalLuminance = 0.0;

        // 遍历所有像素
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                // 提取 sRGB 分量 (0-255)
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // 计算该像素的相对亮度（线性）
                double pixelLuma = calculateLuminance(r, g, b);
                totalLuminance += pixelLuma;
            }
        }

        return totalLuminance / pixelCount;
    }

    /**
     * 计算单个像素的线性亮度（sRGB 标准）
     * @param r 红色分量 (0-255)
     * @param g 绿色分量 (0-255)
     * @param b 蓝色分量 (0-255)
     * @return 相对亮度 Y (0~1)
     */
    private static double calculateLuminance(int r, int g, int b) {
        double rLin = srgbToLinear(r / 255.0);
        double gLin = srgbToLinear(g / 255.0);
        double bLin = srgbToLinear(b / 255.0);
        // ITU-R BT.709 亮度系数 (sRGB 使用该标准)
        return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin;
    }

    /**
     * sRGB 逆伽马变换：将非线性的 sRGB 值转换为线性光强度
     * @param c 归一化的 sRGB 分量值，范围 [0, 1]
     * @return 线性值，范围 [0, 1]
     */
    private static double srgbToLinear(double c) {
        if (c <= 0.04045) {
            return c / 12.92;
        } else {
            return Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }
}
