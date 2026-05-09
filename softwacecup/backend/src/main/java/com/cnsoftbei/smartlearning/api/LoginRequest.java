package com.cnsoftbei.smartlearning.api;

import java.util.List;

public record LoginRequest(String username, String password, String role) {
}
