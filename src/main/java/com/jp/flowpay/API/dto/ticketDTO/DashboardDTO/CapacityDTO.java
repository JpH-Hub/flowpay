package com.jp.flowpay.API.dto.ticketDTO.DashboardDTO;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CapacityDTO {
    private int current;
    private int max;
}
