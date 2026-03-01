package com.lniosy.usedappliance.dto.user;

public record AddressDto(Long id, String receiverName, String receiverPhone, String province, String city,
                         String district, String detailAddress, boolean isDefault) {
}
