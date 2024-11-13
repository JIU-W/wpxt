package com.easypan.utils;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;

public class ScaleFilter {
    private static final Logger logger = LoggerFactory.getLogger(ScaleFilter.class);

    /**
     * 处理图片的缩略图生成，以确保图片在显示时不会过大，提高页面加载速度
     * @param file
     * @param thumbnailWidth
     * @param targetFile
     * @param delSource
     * @return
     */
    public static Boolean createThumbnailWidthFFmpeg(File file, int thumbnailWidth, File targetFile, Boolean delSource) {
        try {
            BufferedImage src = ImageIO.read(file);
            //thumbnailWidth 缩略图的宽度   thumbnailHeight 缩略图的高度
            int sorceW = src.getWidth();
            int sorceH = src.getHeight();
            //小于 指定高宽不压缩
            //如果图片的宽度小于等于指定的缩略图宽度 (thumbnailWidth)，则直接返回 false，表示不需要进行压缩。
            if (sorceW <= thumbnailWidth) {
                return false;
            }
            //调用compressImage方法对图片进行压缩
            compressImage(file, thumbnailWidth, targetFile, delSource);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void compressImageWidthPercentage(File sourceFile, BigDecimal widthPercentage, File targetFile) {
        try {
            BigDecimal widthResult = widthPercentage.multiply(new BigDecimal(ImageIO.read(sourceFile).getWidth()));
            compressImage(sourceFile, widthResult.intValue(), targetFile, true);
        } catch (Exception e) {
            logger.error("压缩图片失败");
        }
    }

    /**
     * 生成视频封面
     * 通过ffmpeg工具从给定的视频文件中提取 一帧 ，然后对提取的帧进行缩放，
     * 生成指定宽度的视频封面，并保存为目标文件
     * @param sourceFile
     * @param width
     * @param targetFile
     */
    public static void createCover4Video(File sourceFile, Integer width, File targetFile) {
        try {
            String cmd = "ffmpeg -i %s -y -vframes 1 -vf scale=%d:%d/a %s";
            ProcessUtils.executeCommand(String.format(cmd, sourceFile.getAbsoluteFile(), width, width, targetFile.getAbsoluteFile()), false);
        } catch (Exception e) {
            logger.error("生成视频封面失败", e);
        }
    }

    /**
     * 通过FFmpeg工具对图片进行压缩，可以设置压缩后的宽度，并且可以选择是否删除原始图片文件
     * @param sourceFile
     * @param width
     * @param targetFile
     * @param delSource
     */
    public static void compressImage(File sourceFile, Integer width, File targetFile, Boolean delSource) {
        try {
            String cmd = "ffmpeg -i %s -vf scale=%d:-1 %s -y";
            ProcessUtils.executeCommand(String.format(cmd, sourceFile.getAbsoluteFile(), width, targetFile.getAbsoluteFile()), false);
            if (delSource) {
                FileUtils.forceDelete(sourceFile);
            }
        } catch (Exception e) {
            logger.error("压缩图片失败");
        }
    }

    public static void main(String[] args) {
        compressImageWidthPercentage(new File("C:\\Users\\Administrator\\Pictures\\微信图片_20230107141436.png"), new BigDecimal(0.7),
                new File("C:\\Users\\Administrator" +
                        "\\Pictures" +
                        "\\微信图片_202106281029182.jpg"));
    }
}