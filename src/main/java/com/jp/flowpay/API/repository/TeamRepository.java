package com.jp.flowpay.API.repository;

import com.jp.flowpay.API.entity.Team;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TeamRepository {

    private final JdbcTemplate jdbcTemplate;

    public TeamRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Team> teamRowMapper = (rs, rowNum) -> {
        Team team = new Team();
        team.setId(rs.getLong("id"));
        team.setName(rs.getString("name"));
        return team;
    };

    public Optional<Team> findById(Long id) {
        String sql = "SELECT * FROM teams WHERE id = ?";
        List<Team> teams = jdbcTemplate.query(sql, teamRowMapper, id);
        return teams.stream().findFirst();
    }

    public Optional<Team> findByNameIgnoreCase(String name) {
        String sql = "SELECT * FROM teams WHERE LOWER(name) = LOWER(?)";
        List<Team> teams = jdbcTemplate.query(sql, teamRowMapper, name);
        return teams.stream().findFirst();
    }

    public List<Team> findAll() {
        String sql = "SELECT * FROM teams ORDER BY id";
        return jdbcTemplate.query(sql, teamRowMapper);
    }
}