package com.takio.HomerTestBot.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Setter
@Entity(name = "adsTable")
public class Ads { // сущность рекламного поста

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // автоматическая генерация id
    private Long id;

    private String ad;
}
