package com.example.campuscrush.alias;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliasMigrationRunner {

    private static final Pattern OLD_ALIAS = Pattern.compile(
        "^(Blue|Red|Purple|Green|Golden|Silver|Crimson|Indigo|Amber|Teal|" +
        "Coral|Jade|Scarlet|Violet|Ivory) " +
        "(Panda|Fox|Otter|Tiger|Falcon|Wolf|Lynx|Raven|Bear|Hawk|" +
        "Deer|Crow|Viper|Crane|Bison|Mole|Newt|Wren|Ibis|Finch)(?: \\d+)?$"
    );

    private final UserRepository userRepository;
    private final AliasGenerator aliasGenerator;

    @PostConstruct
    @Transactional
    public void migrate() {
        List<User> users = userRepository.findAll();
        int count = 0;
        for (User user : users) {
            if (user.getDisplayAlias() != null && OLD_ALIAS.matcher(user.getDisplayAlias()).matches()) {
                user.setDisplayAlias(aliasGenerator.generate());
                userRepository.save(user);
                count++;
            }
        }
        if (count > 0) {
            log.info("AliasMigration: reassigned {} user(s) from old Color-Animal scheme to curated aliases", count);
        }
    }
}
