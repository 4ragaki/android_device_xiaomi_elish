#
# Copyright (C) 2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common lineage stuff.
$(call inherit-product, vendor/aosp/config/common_full_phone.mk)

# Inherit from elish device
$(call inherit-product, device/xiaomi/elish/device.mk)

PRODUCT_NAME := aosp_elish
PRODUCT_DEVICE := elish
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := M2105K81AC

PRODUCT_CHARACTERISTICS := tablet
TARGET_BOOT_ANIMATION_RES := 1440
PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

BUILD_FINGERPRINT := Xiaomi/elish/elish:13/TKQ1.221013.002/V14.0.4.0.TKYCNXM:user/release-keys
