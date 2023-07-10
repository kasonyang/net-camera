#pragma once
#include "SDL.h"

class YuvRender {
private:
    SDL_Renderer* m_Renderer;
    unsigned char* m_FrameBuffer = nullptr;
    unsigned long m_FrameBufferSize = 0;
    SDL_Texture* m_Texture;
    int m_VideoWidth;
    int m_VideoHeight;
    SDL_Rect m_Rect;
    SDL_Event m_Event;
public:
	int init(int video_width, int video_height);
    int render(unsigned char* data[], int pitch[]);
};