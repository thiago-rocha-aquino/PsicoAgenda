package com.psicoagenda.infrastructure.security;

import com.psicoagenda.domain.entity.AdminUser;
import com.psicoagenda.domain.repository.AdminUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public AdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser admin = adminUserRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        if (!admin.isActive()) {
            throw new UsernameNotFoundException("Usuário inativo: " + username);
        }

        return new User(
            admin.getEmail(),
            admin.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
