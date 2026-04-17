package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.entities.TrenEnVivo;
import com.renfetrains.renfetrains.repositories.TrenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trenes")
public class TrenController {

    @Autowired
    private TrenRepository trenRepository;

    @GetMapping("/vivo")
    public List<TrenEnVivo> getTrenesEnVivo() {
        return trenRepository.findAll();
    }
}