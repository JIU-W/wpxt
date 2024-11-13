package com.easypan.controller;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.enums.FileCategoryEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FolderVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;

public class CommonFileController extends ABaseController {

    @Resource
    protected FileInfoService fileInfoService;

    @Resource
    protected AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;


    /**
     * 获取当前目录(查看当前文件夹)
     * @param path
     * @param userId
     * @return
     */
    public ResponseVO getFolderInfo(String path, String userId) {
        String[] pathArray = path.split("/");
        FileInfoQuery infoQuery = new FileInfoQuery();
        infoQuery.setUserId(userId);
        infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        infoQuery.setFileIdArray(pathArray);
        //排序规则：根据参数传递顺序排序   被双引号包裹且被逗号分隔
        // 格式为 and file_id in(?,?) order by field(file_id,"v74rp7qQCr","dJavKIkxRa")
        String orderBy = "field(file_id,\"" + StringUtils.join(pathArray, "\",\"") + "\")";
        infoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FolderVO.class));
    }

    /**
     * 获取图片(即文件下载)     从服务器上读取并返回图片的内容给客户端
     *
     * 传入的文件夹和文件名构建图片文件路径，设置响应头信息，
     * 然后通过调用 readFile 方法将图片文件内容写入响应流返回给客户端。
     * @param response
     * @param imageFolder
     * @param imageName
     */
    public void getImage(HttpServletResponse response, String imageFolder, String imageName) {
        if (StringTools.isEmpty(imageFolder) || StringUtils.isBlank(imageName)) {
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".", "");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=2592000");
        //将图片文件的内容写入响应流，从而返回给客户端。这里可能涉及到文件读取和写入响应流的操作
        readFile(response, filePath);
    }

    /**
     * 视频播放以及普通文件预览
     * @param response
     * @param fileId
     * @param userId
     */
    protected void getFile(HttpServletResponse response, String fileId, String userId) {
        String filePath = null;
        //访问分片文件
        if (fileId.endsWith(".ts")) {
            String[] tsAarray = fileId.split("_");
            String realFileId = tsAarray[0];
            //根据原文件的id查询出一个文件集合
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId, userId);

            if (fileInfo == null) {
                //分享的视频，ts路径记录的是原视频的id,这里通过id直接取出原视频
                FileInfoQuery fileInfoQuery = new FileInfoQuery();
                fileInfoQuery.setFileId(realFileId);
                List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
                fileInfo = fileInfoList.get(0);
                if (fileInfo == null) {
                    return;
                }
                //更具当前用户id和路径去查询当前用户是否有该文件，如果没有直接返回
                fileInfoQuery = new FileInfoQuery();
                fileInfoQuery.setFilePath(fileInfo.getFilePath());
                fileInfoQuery.setUserId(userId);
                Integer count = fileInfoService.findCountByParam(fileInfoQuery);
                if (count == 0) {
                    return;
                }
            }

            //fileName是带有.mp4后缀的原文件  格式为类似于这种(202311/3482411949TuKQBWDG1k.mp4)
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        } else {
            //对于视频文件来说：第一个开始文件视频文件不会走带(.ts)下标的，而是走原文件(带.mp4下标)
            //所以有后面的重新设置文件路径 (.mp4-->.m3u8),从而读取.m3u8文件
            //而.m3u8文件作为 分片文件 的目录文件，接下来就会走 分片文件本身(.ts下标)，即进if语句里。
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
            if (fileInfo == null) {
                return;
            }
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                //视频文件读取.m3u8文件
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                //重新设置文件路径   .mp4-->.m3u8
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                //普通文件读取原文件即可
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        readFile(response, filePath);
    }

    /**
     * 创建下载链接
     * @param fileId
     * @param userId
     * @return
     */
    protected ResponseVO createDownloadUrl(String fileId, String userId) {
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
        //fileInfo == null：只报参数错误就行，不用把原因讲清楚，因为这种一般都是不走界面就想访问后端的。
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //目录(文件夹)不能下载
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //生成随机的长度为50的字符串code
        String code = StringTools.getRandomString(Constants.LENGTH_50);//长度为50
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code);
        downloadFileDto.setFilePath(fileInfo.getFilePath());
        downloadFileDto.setFileName(fileInfo.getFileName());
        //code作为key  fileid作为downloadFileDto
        //存到redis的时候设置过期时间为5分钟
        redisComponent.saveDownloadCode(code, downloadFileDto);

        return getSuccessResponseVO(code);

    }

    /**
     * 下载文件
     * @param request
     * @param response
     * @param code
     * @throws Exception
     */
    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        //从redis中根据key获取值downloadFileDto
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if (null == downloadFileDto) {
            return;
        }

        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        //HTTP响应的内容类型为“application/x-msdownload”，这是一个自定义的内容类型，通常用于微软的下载文件格式。
        //"charset=UTF-8"表示使用UTF-8字符编码
        response.setContentType("application/x-msdownload; charset=UTF-8");
        //检查请求的User-Agent头部，判断是否来自Internet Explorer浏览器。
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
            //如果是，则对文件名进行URL编码；
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            //如果不是，则将文件名转换为ISO 8859-1编码。这是为了处理IE浏览器中可能出现的文件名乱码问题。
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        //设置了HTTP响应头的"Content-Disposition"字段:
        //这告诉浏览器应该将响应的内容作为附件下载，而不是在浏览器中打开。
        // filename 参数设置了下载文件的文件名
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        //从指定文件中读取数据，并将数据通过HttpServletResponse的输出流发送回客户端
        readFile(response, filePath);
    }

}
