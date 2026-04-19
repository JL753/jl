package com.cnsoftbei.smartlearning.api;

import java.util.List;
import java.util.Map;

public record LoginResult(
        String token,
        String role,
        String name,
        String avatar,
        List<Map<String, Object>> menus,
        Map<String, Object> quickProfile
) {
}
