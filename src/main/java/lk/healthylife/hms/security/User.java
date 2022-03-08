package lk.healthylife.hms.security;

import lk.healthylife.hms.dto.FunctionDTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class User implements UserDetails {

    private Long id;
    private String name;
    private String partyCode;
    private Collection<Long> branches;
    private String username;
    private String password;
    private Collection<SimpleGrantedAuthority> authorities;
    private Collection<FunctionDTO> permittedFunctions;
    private String userType;

    public User() {
        this.authorities = new ArrayList<>();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAuthorities(Collection<SimpleGrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    public Collection<FunctionDTO> getPermittedFunctions() {
        return permittedFunctions;
    }

    public void setPermittedFunctions(Collection<FunctionDTO> permittedFunctions) {
        this.permittedFunctions = permittedFunctions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPartyCode() {
        return partyCode;
    }

    public void setPartyCode(String partyCode) {
        this.partyCode = partyCode;
    }

    public Collection<Long> getBranches() {
        return branches;
    }

    public void setBranches(Collection<Long> branches) {
        this.branches = branches;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
