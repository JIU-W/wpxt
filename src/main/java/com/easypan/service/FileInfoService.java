package com.easypan.service;

import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.PaginationResultVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/**
 * 文件信息 业务接口
 */
public interface FileInfoService {

    /**
     * 根据条件查询列表
     */
    List<FileInfo> findListByParam(FileInfoQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(FileInfoQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param);

    /**
     * 新增
     */
    Integer add(FileInfo bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<FileInfo> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<FileInfo> listBean);

    /**
     * 根据FileIdAndUserId查询对象
     */
    FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);


    /**
     * 根据FileIdAndUserId修改
     */
    Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId);


    /**
     * 根据FileIdAndUserId删除
     */
    Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId);

    /**
     * 文件上传
     * @param webUserDto
     * @param fileId
     * @param file
     * @param fileName
     * @param filePid
     * @param fileMd5
     * @param chunkIndex
     * @param chunks
     * @return
     */
    UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex,
                               Integer chunks);

    /**
     * 文件重命名
     * @param fileId
     * @param userId
     * @param fileName
     * @return
     */
    FileInfo rename(String fileId, String userId, String fileName);

    /**
     * 新建文件夹
     * @param filePid
     * @param userId
     * @param folderName
     * @return
     */
    FileInfo newFolder(String filePid, String userId, String folderName);

    void changeFileFolder(String fileIds, String filePid, String userId);

    /**
     * 删除文件
     * @param userId
     * @param fileIds
     */
    void removeFile2RecycleBatch(String userId, String fileIds);

    void recoverFileBatch(String userId, String fileIds);

    //彻底删除
    void delFileBatch(String userId, String fileIds, Boolean adminOp);

    void checkRootFilePid(String rootFilePid, String userId, String fileId);

    void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String cureentUserId);

    Long getUserUseSpace(@Param("userId") String userId);

    void deleteFileByUserId(@Param("userId") String userId);

}