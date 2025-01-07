package com.example.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.data.entity.CommentEntity;
import com.example.data.entity.SongCategoryEntity;
import com.example.data.entity.UserEntity;
import com.example.data.model.CategoryDTO;
import com.example.data.model.CommentRequestDTO;
import com.example.data.model.SongDTO;
import com.example.data.model.converter.SongConverter;
import com.example.data.service.CommentService;
import com.example.data.service.SongService;
import com.example.data.service.UserService;

@Controller
public class MusicController {

    @Autowired
    private SongService songService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @GetMapping("/music/music-page")
    public String musicPage(@RequestParam String username, Model model) {
        model.addAttribute("Songs", songService.getAllSongs());

        model.addAttribute("User", userService.getUserByUsername(username));
        
        return "music/music-page";
    }

    @GetMapping("/music/music-cell")
    public String musicDetail(@RequestParam Integer songId, @RequestParam String username, Model model) {
        model.addAttribute("Songs", songService.getAllSongs());

        model.addAttribute("User", userService.getUserByUsername(username));

        List<CommentEntity> topLevelComments = commentService.getTopLevelCommentsBySongId(songId);
        model.addAttribute("Comments", topLevelComments);

        Map<Integer, List<CommentEntity>> childCommentsMap = new HashMap<>();
        for (CommentEntity topLevelComment : topLevelComments) {
            List<CommentEntity> childComments = commentService.getCommentsByParentCommentId(topLevelComment.getComment_id());
            childCommentsMap.put(topLevelComment.getComment_id(), childComments);
        }
        model.addAttribute("ChildCommentsMap", childCommentsMap);

        List<SongDTO> songWithCategory = songService.getAllSongs();
        SongDTO song = songWithCategory.stream().filter(s -> s.getSongId() == songId).findFirst().orElse(null);
        model.addAttribute("Song", song);

        return "music/music-cell";
    }

    @GetMapping("/music/song-detail")
    @ResponseBody
    public List<SongDTO> songDetail() {
        return songService.getAllSongs();
    }

    @GetMapping("/music/category")
    @ResponseBody
    public List<SongDTO> songDetail(@RequestParam Integer categoryId) {
        List<SongDTO> songs = songService.getAllSongs();
        CategoryDTO songCategory = songService.getCategoryById(categoryId);
        List<SongDTO> res = new ArrayList<>();

        for (SongDTO song : songs) {
            if (song.getCategories().contains(songCategory)) {
                res.add(song);
            }
        }

        return res;
    }

    @GetMapping("/music/category-song")
    @ResponseBody
    public List<SongDTO> songDetail(@RequestParam String categoryName) {
        return songService.getSongByCategoryName(categoryName);
    }

    @PostMapping("/user/music/music-cell/comment")
    public ResponseEntity<Boolean> addComment(@RequestBody CommentRequestDTO commentDto) {
        // Print the commentDto to the console
        System.out.println("Comment: " + commentDto.getCommentText());
        System.out.println("UserID: " + commentDto.getUserId());
        System.out.println("FilmID: " + commentDto.getFilmId());


        UserEntity user = userService.getUserById(commentDto.getUserId());
        if (user == null) {
            return ResponseEntity.badRequest().body(false);
        }

        SongDTO song = (SongDTO) songService.getSongById(commentDto.getSongId());
        if (song == null) {
            return ResponseEntity.badRequest().body(false);
        }

        CommentEntity parentComment = commentService.getCommentById(commentDto.getParentCommentId());
        if (parentComment != null) {
            System.err.println("Can not find parent comment!!");
        }

        // Create a new CommentEntity
        CommentEntity commentEntity = new CommentEntity();
        commentEntity.setUser(user);
        commentEntity.setFilm(null);
        commentEntity.setSongEntity(SongConverter.convertToEntity(song));
        commentEntity.setComment_text(commentDto.getCommentText());
        commentEntity.setType(commentDto.getType());
        commentEntity.setTime_rated(commentDto.getTimeRated());
        commentEntity.setParentComment(commentService.getCommentById(commentDto.getParentCommentId()));

        commentService.saveComment(commentEntity);

        return ResponseEntity.ok(true);
    }
    
}
