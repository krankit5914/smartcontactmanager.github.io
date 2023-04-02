package com.smart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.entity.User;
import com.smart.helper.Message;
import com.smart.repo.UserRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @RequestMapping("/")
    public String home(Model model){
        model.addAttribute("title", "HOME - Smart contact manager");
        return "home";
    }
    @RequestMapping("/about")
    public String about(Model model){
        model.addAttribute("title", "ABOUT - Smart contact manager");
        return "about";
    }
    @RequestMapping("/signup")
    public String signup(Model model){
        model.addAttribute("title", "REGISTER - Smart contact manager");
        model.addAttribute("user",new User());
        return "signup";
    }
    @RequestMapping("/signin")
    public String customelogin(Model model){
        model.addAttribute("title", "Login - Smart contact manager");
        return "login";
    }

    // heandler for registering user
    @RequestMapping(value = "/do_register", method = RequestMethod.POST)
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result1, @RequestParam(value="agreement", defaultValue="false") boolean agreement,Model model,HttpSession session){
       try {
        if(!agreement){
            System.out.println("you have not agreed trems and condition");
            throw new Exception("you have not agreed trems and condition");
        }

        if(result1.hasErrors()){
            System.out.println("Error : "+result1.toString());
            model.addAttribute("user", user);
            return "signup";
        }

        user.setRole("ROLE_USER");
        user.setEnabled(true);
        user.setImageUrl("Defoult.jpg");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User result= this.userRepository.save(user);

        System.out.println(" Agreement "+ agreement);
        System.out.println(" User "+result);
        model.addAttribute("User", new User());
        
        session.setAttribute("message", new Message("seccussfully register !!","alert-success"));
        return "signup";
       } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("user", user);
            session.setAttribute("message", new Message("someting went wrong !!"+e.getMessage(),"alert-danger"));
            return "signup";
       }
    }
}
