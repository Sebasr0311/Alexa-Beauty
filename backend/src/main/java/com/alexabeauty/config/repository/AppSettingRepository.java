package com.alexabeauty.config.repository;

import com.alexabeauty.config.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, UUID> {
    Optional<AppSetting> findBySettingKey(String settingKey);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM AppSetting s WHERE s.settingKey = 'business_name'")
    Optional<AppSetting> lockBootstrapSetting();
}
