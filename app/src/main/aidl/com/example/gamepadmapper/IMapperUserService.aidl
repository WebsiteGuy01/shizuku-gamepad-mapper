package com.example.gamepadmapper;

interface IMapperUserService {
    void injectDown(float x, float y);
    void injectMove(float x, float y);
    void injectUp(float x, float y);
    void destroy();
}
