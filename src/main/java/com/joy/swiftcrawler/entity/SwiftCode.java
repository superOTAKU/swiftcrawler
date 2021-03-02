package com.joy.swiftcrawler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName("swift_code")
@EqualsAndHashCode(of = "swiftCode")
public class SwiftCode {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String swiftCode;
    private String country;
    private String bank;
    private String branch;
    private String city;
    private String zipcode;
    private String address;

}
