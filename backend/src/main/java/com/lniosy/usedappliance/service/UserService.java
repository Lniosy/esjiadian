package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.user.*;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.entity.UserAddress;
import com.lniosy.usedappliance.entity.UserAuth;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import com.lniosy.usedappliance.mapper.UserAddressMapper;
import com.lniosy.usedappliance.mapper.UserAuthMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final SysUserMapper sysUserMapper;
    private final UserAddressMapper userAddressMapper;
    private final UserAuthMapper userAuthMapper;

    public UserService(SysUserMapper sysUserMapper, UserAddressMapper userAddressMapper, UserAuthMapper userAuthMapper) {
        this.sysUserMapper = sysUserMapper;
        this.userAddressMapper = userAddressMapper;
        this.userAuthMapper = userAuthMapper;
    }

    public UserProfileDto getProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return new UserProfileDto(
                user.getId(), user.getNickname(), user.getAvatarUrl(), user.getBio(),
                user.getRoles(), user.getAuthStatus(), user.getEnabled());
    }

    public UserPublicProfileDto getPublicProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BizException(404, "用户不存在");
        }
        return new UserPublicProfileDto(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getAuthStatus()
        );
    }

    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest req) {
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getNickname, req.nickname())
                .set(SysUser::getAvatarUrl, req.avatarUrl())
                .set(SysUser::getBio, req.bio()));
        return getProfile(userId);
    }

    public List<AddressDto> listAddresses(Long userId) {
        return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getId))
                .stream().map(this::toAddressDto).toList();
    }

    public AddressDto addAddress(Long userId, AddressRequest req) {
        if (req.isDefault()) {
            clearDefaultAddress(userId);
        }
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(req.receiverName());
        address.setReceiverPhone(req.receiverPhone());
        address.setProvince(req.province());
        address.setCity(req.city());
        address.setDistrict(req.district());
        address.setDetailAddress(req.detailAddress());
        address.setIsDefault(req.isDefault());
        userAddressMapper.insert(address);
        return toAddressDto(address);
    }

    public AddressDto updateAddress(Long userId, Long addressId, AddressRequest req) {
        UserAddress old = userAddressMapper.selectById(addressId);
        if (old == null || !old.getUserId().equals(userId)) {
            throw new BizException(404, "地址不存在");
        }
        if (req.isDefault()) {
            clearDefaultAddress(userId);
        }
        old.setReceiverName(req.receiverName());
        old.setReceiverPhone(req.receiverPhone());
        old.setProvince(req.province());
        old.setCity(req.city());
        old.setDistrict(req.district());
        old.setDetailAddress(req.detailAddress());
        old.setIsDefault(req.isDefault());
        userAddressMapper.updateById(old);
        return toAddressDto(old);
    }

    public void deleteAddress(Long userId, Long addressId) {
        UserAddress old = userAddressMapper.selectById(addressId);
        if (old == null || !old.getUserId().equals(userId)) {
            throw new BizException(404, "地址不存在");
        }
        userAddressMapper.deleteById(addressId);
    }

    public String applyRealName(Long userId, RealNameVerifyRequest req) {
        String masked = maskIdCard(req.idCard());
        UserAuth existing = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getUserId, userId)
                .last("limit 1"));
        if (existing == null) {
            UserAuth auth = new UserAuth();
            auth.setUserId(userId);
            auth.setRealName(req.realName());
            auth.setIdCardMasked(masked);
            auth.setStatus("PENDING");
            userAuthMapper.insert(auth);
        } else {
            existing.setRealName(req.realName());
            existing.setIdCardMasked(masked);
            existing.setStatus("PENDING");
            existing.setRejectReason(null);
            userAuthMapper.updateById(existing);
        }
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getAuthStatus, "PENDING"));
        return "PENDING";
    }

    public String realNameStatus(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user.getAuthStatus();
    }

    public void approveRealName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        String roles = user.getRoles().contains("ROLE_SELLER") ? user.getRoles() : user.getRoles() + ",ROLE_SELLER";
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getAuthStatus, "APPROVED")
                .set(SysUser::getRoles, roles));
        userAuthMapper.update(null, new LambdaUpdateWrapper<UserAuth>()
                .eq(UserAuth::getUserId, userId)
                .set(UserAuth::getStatus, "APPROVED")
                .set(UserAuth::getRejectReason, null));
    }

    public void rejectRealName(Long userId) {
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getAuthStatus, "REJECTED"));
        userAuthMapper.update(null, new LambdaUpdateWrapper<UserAuth>()
                .eq(UserAuth::getUserId, userId)
                .set(UserAuth::getStatus, "REJECTED"));
    }

    private void clearDefaultAddress(Long userId) {
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, false));
    }

    private AddressDto toAddressDto(UserAddress a) {
        return new AddressDto(a.getId(), a.getReceiverName(), a.getReceiverPhone(), a.getProvince(), a.getCity(),
                a.getDistrict(), a.getDetailAddress(), Boolean.TRUE.equals(a.getIsDefault()));
    }

    private String maskIdCard(String idCard) {
        if (idCard.length() < 8) {
            return "****";
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
}
