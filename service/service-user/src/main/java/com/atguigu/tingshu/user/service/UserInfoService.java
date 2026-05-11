package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    Map<String, String> wxLogin(String code);

    UserInfoVo getUserInfo(Long userId);

    void updateUser(Long userId, UserInfoVo userInfoVo);

    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList);

    Boolean userIsPaidAlbum(Long userId, Long albumId);

    List<Long> findUserPaidTrackIdList(Long userId, Long albumId);

    void savePaidRecord(UserPaidRecordVo userPaidRecordVo);

    void updateVipExpireStatus(Date now);
}
