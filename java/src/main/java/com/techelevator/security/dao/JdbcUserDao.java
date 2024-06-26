package com.techelevator.security.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.techelevator.exception.DaoException;
import com.techelevator.security.model.Authority;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.techelevator.model.UserProfile;
import com.techelevator.security.model.RegisterUserDto;
import com.techelevator.security.model.User;

@Component
public class JdbcUserDao implements UserDao {

  private final JdbcTemplate jdbcTemplate;

  public JdbcUserDao(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public User getUserById(int userId) {
    User user = null;
    String sql = "SELECT user_id, username, password_hash, role FROM users WHERE user_id = ?";
    try {
      SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId);
      if (results.next()) {
        user = mapRowToUser(results);
      }
    } catch (CannotGetJdbcConnectionException e) {
      throw new DaoException("Unable to connect to server or database", e);
    }
    return user;
  }

  @Override
  public List<User> getUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT user_id, username, password_hash, role FROM users";
    try {
      SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
      while (results.next()) {
        User user = mapRowToUser(results);
        users.add(user);
      }
    } catch (CannotGetJdbcConnectionException e) {
      throw new DaoException("Unable to connect to server or database", e);
    }
    return users;
  }

  @Override
  public User getUserByUsername(String username) {
    if (username == null)
      throw new IllegalArgumentException("Username cannot be null");
    User user = null;
    String sql = "SELECT user_id, username, password_hash, role FROM users WHERE username = LOWER(TRIM(?));";
    try {
      SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
      if (rowSet.next()) {
        user = mapRowToUser(rowSet);
      }
    } catch (CannotGetJdbcConnectionException e) {
      throw new DaoException("Unable to connect to server or database", e);
    }
    return user;
  }

  @Override
  public User createUser(RegisterUserDto user) {
    User newUser = null;
    String insertUserSql = "INSERT INTO users (username, password_hash, role) values (LOWER(TRIM(?)), ?, ?) RETURNING user_id";
    String password_hash = new BCryptPasswordEncoder().encode(user.getPassword());
    String ssRole = user.getRole().toUpperCase().startsWith("ROLE_") ? user.getRole().toUpperCase()
        : "ROLE_" + user.getRole().toUpperCase();
    try {
      int newUserId = jdbcTemplate.queryForObject(insertUserSql, int.class, user.getUsername(), password_hash, ssRole);
      newUser = getUserById(newUserId);
    } catch (CannotGetJdbcConnectionException e) {
      throw new DaoException("Unable to connect to server or database", e);
    } catch (DataIntegrityViolationException e) {
      throw new DaoException("Data integrity violation", e);
    }
    return newUser;
  }


  @Override
  public User makeUserEmployee(int id) {
      User user = getUserById(id);
      user.setAuthorities("ROLE_EMPLOYEE");

      String sql = "update users set role = ? where user_id = ?";
      try {
        int rows = jdbcTemplate.update(sql, "ROLE_EMPLOYEE", id);
        if(rows > 0) {
          return user;
        }
        throw new DaoException("something went wrong");
      } catch (CannotGetJdbcConnectionException e) {
        throw new DaoException("Unable to connect", e);
      } catch (DataIntegrityViolationException e) {
        throw new DaoException("data integridy violation", e);
      }

  }

  @Override
  public void fireEmployee(int id) {
    String sql = "update users set role = ? where user_id = ?";

    try {
      int rows = jdbcTemplate.update(sql, "ROLE_USER", id);
    } catch (CannotGetJdbcConnectionException e) {
      throw new DaoException("Unable to connect", e);
    } catch (DataIntegrityViolationException e) {
      throw new DaoException("data integridy violation", e);
    }
  }

  @Override
  public void banMember(int id) {
    User user = getUserById(id);
    user.setAuthorities("ROLE_BANNED");

    String sql = "delete from users where user_id = ?";
   String deleteUserSQL = "delete from user_profiles where user_id = ?;";
    String deleteCheckinSql = "delete from checkins where user_id = ?;";

    try {
      jdbcTemplate.update(deleteCheckinSql, id);
      jdbcTemplate.update(deleteUserSQL,id);
      int rows = jdbcTemplate.update(sql, id);


    } catch (CannotGetJdbcConnectionException e) {
      throw new DaoException("Unable to connect", e);
    } catch (DataIntegrityViolationException e) {
      throw new DaoException("data integridy violation", e);
    }
  }
  private User mapRowToUser(SqlRowSet rs) {
    User user = new User();
    user.setId(rs.getInt("user_id"));
    user.setUsername(rs.getString("username"));
    user.setPassword(rs.getString("password_hash"));
    user.setAuthorities(Objects.requireNonNull(rs.getString("role")));
    user.setActivated(true);
    return user;
  }
}
