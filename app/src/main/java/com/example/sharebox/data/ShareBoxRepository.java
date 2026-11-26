package com.example.sharebox.data;

import com.example.sharebox.data.Models.State;

public interface ShareBoxRepository {
    State demoState();
    void authenticate(String email, String password, boolean registration);
    String answer(String question);
}
