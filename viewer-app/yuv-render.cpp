#include "yuv-render.h"
#include "SDL.h"
#include <stdio.h>

int YuvRender::init(int video_width, int video_height) {
    SDL_Init(SDL_INIT_VIDEO);
    SDL_Rect bounds;
    SDL_GetDisplayUsableBounds(0, &bounds);
    //printf("usable bonuds:w=%d,h=%d\n", bounds.w, bounds.h);
    int winWidth = video_width;
    int winHeight = video_height;
    if (winWidth > bounds.w || winHeight > bounds.h) {
        float widthRatio = 1.0 * winWidth / bounds.w;
        float heightRatio = 1.0 * winHeight / bounds.h;
        float maxRatio = widthRatio > heightRatio ? widthRatio : heightRatio;
        winWidth = int(winWidth / maxRatio);
        winHeight = int(winHeight / maxRatio);
    }
    SDL_Window* window = SDL_CreateWindow(
        "NetCameraViewer",
        SDL_WINDOWPOS_UNDEFINED,
        SDL_WINDOWPOS_UNDEFINED,
        winWidth,
        winHeight,
        SDL_WINDOW_OPENGL
    );
    m_Renderer = SDL_CreateRenderer(window, -1, 0);
    m_Texture = SDL_CreateTexture(
        m_Renderer,
        SDL_PIXELFORMAT_IYUV,
        SDL_TEXTUREACCESS_STREAMING,
        video_width,
        video_height
    );
    m_VideoWidth = video_width;
    m_VideoHeight = video_height;
    m_Rect.x = 0;
    m_Rect.y = 0;
    m_Rect.w = winWidth;
    m_Rect.h = winHeight;
    return 0;
}

int YuvRender::render(unsigned char* data[], int pitch[]) {
    int uvHeight = m_VideoHeight / 2;
    int ySize = pitch[0] * m_VideoHeight;
    int uSize = pitch[1] * uvHeight;
    int vSize = pitch[2] * uvHeight;
    int buffSize =  ySize + uSize + vSize;
    if (m_FrameBufferSize < buffSize) {
        if (m_FrameBuffer != nullptr) {
            delete[] m_FrameBuffer;
        }
        m_FrameBuffer = new unsigned char[buffSize];
        m_FrameBufferSize = buffSize;
    }
    SDL_memcpy(m_FrameBuffer, data[0], ySize);
    SDL_memcpy(m_FrameBuffer + ySize, data[1], uSize);
    SDL_memcpy(m_FrameBuffer + ySize + uSize, data[2], vSize);
    SDL_UpdateTexture(m_Texture, NULL, m_FrameBuffer, pitch[0]);
    SDL_RenderClear(m_Renderer);
    SDL_RenderCopy(m_Renderer, m_Texture, NULL, &m_Rect);
    SDL_RenderPresent(m_Renderer);
    SDL_PollEvent(&m_Event);
    if (m_Event.type == SDL_QUIT) {
        exit(0);
    }
    return 0;
}
