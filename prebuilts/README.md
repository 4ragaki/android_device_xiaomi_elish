[credit:github.com/web1n/android_device_xiaomi_elish-kernel-prebuilt](https://github.com/web1n/android_device_xiaomi_elish-kernel-prebuilt)

## Patch dtbo.img to fix physical panel dimensions

### Patch

1. dump dtbo.img to dtb: `mkdtimg dump dtbo-orig.img -b test`
2. patch test.22:
```shell
sed -i 's/\x05\xC2/\x00\x93/g' test.22
sed -i 's/\x09\x37/\x00\xEC/g' test.22
```
3. dtb to dtbo.img: `mkdtimg create dtbo.img test.*`

### Result

```shell
elish:/ $ dumpsys display | grep density                                                       
  DisplayDeviceInfo{"内置屏幕": uniqueId="local:4630946545580055169", 1600 x 2560, modeId 2, defaultModeId 1, supportedModes [{id=1, width=1600, height=2560, fps=120.00001, alternativeRefreshRates=[30.000002, 60.000004]}, {id=2, width=1600, height=2560, fps=60.000004, alternativeRefreshRates=[30.000002, 120.00001]}, {id=3, width=1600, height=2560, fps=30.000002, alternativeRefreshRates=[60.000004, 120.00001]}], colorMode 0, supportedColorModes [0], hdrCapabilities HdrCapabilities{mSupportedHdrTypes=[2, 3, 4], mMaxLuminance=420.0, mMaxAverageLuminance=210.1615, mMinLuminance=0.323}, allmSupported false, gameContentTypeSupported false, density 275, 276.462 x 275.525 dpi, appVsyncOff 1000000, presDeadline 16666666, touch INTERNAL, rotation 0, type INTERNAL, address {port=129, model=0x40446d58ef1f1a}, deviceProductInfo DeviceProductInfo{name=, manufacturerPnpId=QCM, productId=1, modelYear=null, manufactureDate=ManufactureDate{week=27, year=2006}, connectionToSinkType=0}, state OFF, committedState OFF, frameRateOverride , brightnessMinimum 0.0, brightnessMaximum 1.0, brightnessDefault 0.2627451, FLAG_ALLOWED_TO_BE_DEFAULT_DISPLAY, FLAG_ROTATES_WITH_CONTENT, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS, installOrientation 0}
```
