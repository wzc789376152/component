package com.github.wzc789376152.springboot.taskCenter.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.github.wzc789376152.springboot.annotation.TableFieldType;
import com.github.wzc789376152.springboot.enums.FileType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 任务中心
 * </p>
 *
 * @author Weizhenchen
 * @since 2022-05-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("db_taskcenter")
@ApiModel(value = "Taskcenter对象", description = "任务中心")
public class Taskcenter implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "服务包名")
    private String serviceName;

    @ApiModelProperty(value = "服务方法名")
    private String serviceMethod;

    @ApiModelProperty(value = "服务回调方法名")
    private String callbackServiceMethod;

    @ApiModelProperty(value = "服务参数")
    private String serviceParam;

    @ApiModelProperty(value = "错误信息")
    private String errorMsg;

    @ApiModelProperty(value = "进度")
    private Integer progress;

    @ApiModelProperty(value = "状态1、进行中2、等待中3、错误4、成功")
    private Integer status;
    
    @ApiModelProperty(value = "下载url")
    private String url;

    @ApiModelProperty(value = "完成时间")
    private Date finishTime;

    @ApiModelProperty(value = "是否删除")
    @TableLogic
    private Boolean isDeleted;

    @ApiModelProperty(value = "创建人")
    @TableField(fill = FieldFill.INSERT)
    @TableFieldType(FileType.Author)
    private String creator;
    @ApiModelProperty(value = "创建人Id")
    @TableField(fill = FieldFill.INSERT)
    @TableFieldType(FileType.AuthorId)
    private Integer creatorId;

    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    @TableFieldType(FileType.DateTime)
    private Date gmtCreated;

}
