package com.landit.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 复盘维度实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_review_dimension")
public class ReviewDimension extends BaseEntity {

    private String reviewId;

    private String name;

    private Integer score;

    private Integer maxScore;

    private String feedback;

}
