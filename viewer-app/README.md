# Windows下构建

## 环境准备

下载依赖

1. 最新的ffmpeg(win64-gpl-shared版本)：https://github.com/BtbN/FFmpeg-Builds/releases
2. 最新的SDL2(devel-vc版本)：https://github.com/libsdl-org/SDL/releases


创建`third-part`目录，然后把下载下来的ffmpeg和SDL2解压到`third-part`目录里，ffmpeg-xxxx目录需要重命名为`ffmpeg`。

最终，将得到类似的目录结构：

```
third-part/ffmpeg/include/
third-part/ffmpeg/lib/
third-part/SDL2-xxxx/include/
third-part/SDL2-xxxx/lib/
...
```

## 构建

```
mkdir build
cd build
cmake ..
cmake --build .
```
