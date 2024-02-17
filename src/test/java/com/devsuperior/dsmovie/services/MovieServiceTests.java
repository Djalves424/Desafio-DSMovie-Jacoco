package com.devsuperior.dsmovie.services;

import com.devsuperior.dsmovie.dto.MovieDTO;
import com.devsuperior.dsmovie.entities.MovieEntity;
import com.devsuperior.dsmovie.repositories.MovieRepository;
import com.devsuperior.dsmovie.services.exceptions.DatabaseException;
import com.devsuperior.dsmovie.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dsmovie.tests.MovieFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
public class MovieServiceTests {

    @InjectMocks
    private MovieService service;
    @Mock
    private MovieRepository repository;

    private MovieEntity movie;
    private MovieDTO movieDTO;
    private String existingMovieTitle;
    private Long existingMovieId, nonExistingMovieId, dependentMovieId;

    private PageImpl<MovieEntity> page;
    private Pageable pageable;

    @BeforeEach
    void setUp() throws Exception {

        movie = MovieFactory.createMovieEntity();
        movieDTO = MovieFactory.createMovieDTO();
        pageable = PageRequest.of(0, 10);
        page = new PageImpl<>(List.of(movie));

        existingMovieTitle = movie.getTitle();
        existingMovieId = 1L;
        nonExistingMovieId = 2L;
        dependentMovieId = 3L;

        Mockito.when(repository.searchByTitle(existingMovieTitle, pageable)).thenReturn(page);

        Mockito.when(repository.findById(existingMovieId)).thenReturn(Optional.of(movie));
        Mockito.when(repository.findById(nonExistingMovieId)).thenReturn(Optional.empty());

        Mockito.when(repository.getReferenceById(existingMovieId)).thenReturn(movie);
        Mockito.when(repository.getReferenceById(nonExistingMovieId)).thenThrow(ResourceNotFoundException.class);

        Mockito.when(repository.save(any())).thenReturn(movie);

        Mockito.when(repository.existsById(existingMovieId)).thenReturn(true);
        Mockito.when(repository.existsById(nonExistingMovieId)).thenReturn(false);
        Mockito.when(repository.existsById(dependentMovieId)).thenReturn(true);

        Mockito.doNothing().when(repository).deleteById(existingMovieId);
        Mockito.doThrow(DataIntegrityViolationException.class).when(repository).deleteById(dependentMovieId);
    }

    @Test
    public void findAllShouldReturnPagedMovieDTO() {

        Page<MovieDTO> result = service.findAll(movie.getTitle(), pageable);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getSize(), 1);
        Assertions.assertEquals(result.iterator().next().getTitle(), existingMovieTitle);
    }

    @Test
    public void findByIdShouldReturnMovieDTOWhenIdExists() {

        MovieDTO dto = service.findById(existingMovieId);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals(dto.getId(), existingMovieId);
        Assertions.assertEquals(dto.getTitle(), movie.getTitle());
    }

    @Test
    public void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.findById(nonExistingMovieId);
        });
    }

    @Test
    public void insertShouldReturnMovieDTO() {

        MovieDTO result = service.insert(movieDTO);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getId(), movie.getId());
        Assertions.assertEquals(result.getTitle(), movie.getTitle());
    }

    @Test
    public void updateShouldReturnMovieDTOWhenIdExists() {

        MovieDTO result = service.update(existingMovieId, movieDTO);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getId(), existingMovieId);
        Assertions.assertEquals(result.getTitle(), movieDTO.getTitle());
    }

    @Test
    public void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.update(nonExistingMovieId, movieDTO);
        });
    }

    @Test
    public void deleteShouldDoNothingWhenIdExists() {

        Assertions.assertDoesNotThrow(() -> {
            service.delete(existingMovieId);
        });
    }

    @Test
    public void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.delete(nonExistingMovieId);
        });
    }

    @Test
    public void deleteShouldThrowDatabaseExceptionWhendependentMovieId() {

        Assertions.assertThrows(DatabaseException.class, () -> {
            service.delete(dependentMovieId);
        });
    }
}
