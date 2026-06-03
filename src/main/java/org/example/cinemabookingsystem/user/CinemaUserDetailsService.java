package org.example.cinemabookingsystem.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CinemaUserDetailsService implements UserDetailsService {

    private final CinemaUserRepository cinemaUserRepository;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        return cinemaUserRepository.findByUserName(userName)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userName));
    }

    private UserDetails toUserDetails(CinemaUser user) {
        return User.builder()
                .username(user.userName())
                .password(user.password())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.role().name()))
                .build();
    }
}
