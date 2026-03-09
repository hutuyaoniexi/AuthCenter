package com.box.auth.dto;

/**
 * token对象
 *
 * @param accessToken
 * @param refreshToken
 */
public record TokenPair(String accessToken, String refreshToken) {
}
