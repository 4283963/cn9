package com.rocket.simulation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SimulationRequest {

    @NotBlank(message = "配方名称不能为空")
    @Size(max = 100, message = "配方名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    @NotNull(message = "液氧比例不能为空")
    @DecimalMin(value = "0.1", message = "液氧比例至少为0.1")
    @DecimalMax(value = "10.0", message = "液氧比例最大为10.0")
    private Double loxRatio;

    @NotNull(message = "煤油比例不能为空")
    @DecimalMin(value = "0.1", message = "煤油比例至少为0.1")
    @DecimalMax(value = "10.0", message = "煤油比例最大为10.0")
    private Double keroseneRatio;

    @NotNull(message = "燃烧室压力不能为空")
    @DecimalMin(value = "1.0", message = "燃烧室压力至少为1 MPa")
    @DecimalMax(value = "50.0", message = "燃烧室压力最大为50 MPa")
    private Double chamberPressure;

    @NotNull(message = "初始温度不能为空")
    @DecimalMin(value = "200.0", message = "初始温度至少为200 K")
    @DecimalMax(value = "800.0", message = "初始温度最大为800 K")
    private Double initialTemperature;

    @NotNull(message = "燃烧时长不能为空")
    @DecimalMin(value = "1.0", message = "燃烧时长至少为1秒")
    @DecimalMax(value = "600.0", message = "燃烧时长最大为600秒")
    private Double burnDuration;

    @NotNull(message = "喷管面积比不能为空")
    @DecimalMin(value = "1.0", message = "喷管面积比至少为1.0")
    @DecimalMax(value = "100.0", message = "喷管面积比最大为100.0")
    private Double nozzleAreaRatio;
}
