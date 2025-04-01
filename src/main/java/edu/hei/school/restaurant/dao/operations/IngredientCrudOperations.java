package edu.hei.school.restaurant.dao.operations;

import edu.hei.school.restaurant.dao.DataSource;
import edu.hei.school.restaurant.model.Ingredient;
import edu.hei.school.restaurant.dao.mapper.IngredientMapper;
import edu.hei.school.restaurant.service.exception.NotFoundException;
import edu.hei.school.restaurant.service.exception.ServerException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class IngredientCrudOperations implements CrudOperations<Ingredient> {
    private final DataSource dataSource;
    private final IngredientMapper ingredientMapper;

    @Override
    public List<Ingredient> getAll(int page, int size) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Ingredient findById(Long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select i.id, i.name, di.id as dish_ingredient_id, di.required_quantity, di.unit from ingredient i"
                             + " join dish_ingredient di on i.id = di.id_ingredient"
                             + " where i.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return ingredientMapper.apply(resultSet);
                }
                throw new NotFoundException("Ingredient.id=" + id + " not found");
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    @SneakyThrows
    @Override
    public List<Ingredient> saveAll(List<Ingredient> entities) {
        List<Ingredient> ingredients = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement =
                         connection.prepareStatement("insert into ingredient (name) values (?)"
                                 + " on conflict (id) do update set name=excluded.name"
                                 + " returning id, name")) {
                entities.forEach(entityToSave -> {
                    try {
                        statement.setString(1, entityToSave.getName());
                        statement.addBatch(); // group by batch so executed as one query in database
                    } catch (SQLException e) {
                        throw new ServerException(e);
                    }
                });
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        ingredients.add(ingredientMapper.apply(resultSet));
                    }
                }
                return ingredients;
            }
        }
    }
}
