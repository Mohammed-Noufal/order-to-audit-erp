package com.orderaudit.order_to_audit_erp.master;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "settings")
public class Setting {

    @Id
    @Column(name = "setting_key", length = 50)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 100)
    private String settingValue;

    protected Setting() {
        // required by JPA
    }

    public Setting(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public String getSettingKey() { return settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
}
