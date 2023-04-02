package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.entity.Contact;
import com.smart.entity.User;
import com.smart.helper.Message;
import com.smart.repo.ContactRepository;
import com.smart.repo.UserRepository;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    // method from adding common data to response
    @ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        String userName = principal.getName();
        System.out.println("USERNAME : " + userName);

        User user = this.userRepository.getUserByUserName(userName);
        System.out.println(user);

        model.addAttribute("user", user);
    }

    // dashboard
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("title", "User Dashboard");
        return "normal/user_dashboard";
    }

    // add contact form heandler
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model) {
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    // adding process contact
    @PostMapping("/process-contact")
    public String ProcessContact(@ModelAttribute @Valid Contact contact,  
            @RequestParam("profileImage") MultipartFile file, BindingResult bindingResult, Principal principal,
            HttpSession session, Model model) {

        try {

            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);

            // processing and uploading file
            if (file.isEmpty()) {
                // if the file is empty then try our message
                System.out.println("file is emapty");
                contact.setImage("contact.png");
            } else {
                // file the file to the folder and update the name and contact
                contact.setImage(file.getOriginalFilename());
                File saveFile = new ClassPathResource("static/img").getFile();

                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }

            user.getContacts().add(contact);
            contact.setUser(user);
            this.userRepository.save(user);

            System.out.println("Data:" + contact);
            System.out.println("added to database");

            session.setAttribute("message",
                    new Message("seccussfully Added your contact !! Add more...", "alert-success"));
            return "normal/add_contact_form";
        } catch (Exception e) {
            System.out.println("Error :" + e.getMessage());
            e.printStackTrace();
            session.setAttribute("message",
                    new Message("someting went wrong !! Try again..." + e.getMessage(), "alert-danger"));
            return "normal/add_contact_form";
        }

    }

    // view contact form heandler
    @GetMapping("/view-contact/{page}")
    public String openviewContactForm(@PathVariable("page") Integer page, Model model, Principal principal) {
        model.addAttribute("title", "View Contact");
        
        String userName=principal.getName();
        User user=this.userRepository.getUserByUserName(userName);
        Pageable pageable=PageRequest.of(page, 8);
        Page<Contact> contacts=this.contactRepository.findContactByUser(user.getId(), pageable);
        model.addAttribute("contacts", contacts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contacts.getTotalPages());
        return "normal/view_contact_form";
    }


    // showing particular contact details
    // @RequestMapping("/contact/{cId}")
    @GetMapping("/{cId}/contact")
    public String showContactDetails(@PathVariable("cId") Integer cId, Model model,Principal principal){
        System.out.println("CID"+cId);
        Optional<Contact> optionalContact =this.contactRepository.findById(cId);
        Contact contact =optionalContact.get();
        String userName =principal.getName();
        User user=this.userRepository.getUserByUserName(userName);
        if(user.getId()==contact.getUser().getId()){ 
            model.addAttribute("contact", contact);
            model.addAttribute("title", contact.getName());
        }
        return "normal/contact_detail";
    }

    // delete contact heandler
    @GetMapping("/delete/{cId}")
    public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session, Principal principal){
       
        Contact contact =this.contactRepository.findById(cId).get();
        System.out.println("contact "+ contact.getcId());
        
        User user = this.userRepository.getUserByUserName(principal.getName());
        user.getContacts().remove(contact);
        this.userRepository.save(user);
        System.out.println("DELETED");
        return "redirect:/user/view-contact/0";
    }

    // update contact heandler
    @PostMapping("/update-contact/{cId}")
    public String updateContact(@PathVariable("cId")Integer cId, Model model){
        model.addAttribute("title", "Update Contact");
        Contact contact = this.contactRepository.findById(cId).get();
        model.addAttribute("contact", contact);
        return "normal/update_form";
    }
    
    // data updat heandler
    @RequestMapping(value = "/process-update", method = RequestMethod.POST)
    public String updateHeandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Model model, HttpSession session, Principal principal){
        try {
            // get old contact detail
            Contact oldContact=this.contactRepository.findById(contact.getcId()).get();
            if(!file.isEmpty()){
                // delete old contact pp
                File deleteFile = new ClassPathResource("static/img").getFile(); 
                File file1=new File(deleteFile, oldContact.getImage());
                file1.delete();
                // add new contact pp
                File saveFile = new ClassPathResource("static/img").getFile(); 
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                contact.setImage(file.getOriginalFilename());
            }else{
                contact.setImage(oldContact.getImage());
            }
            User user = this.userRepository.getUserByUserName(principal.getName());
            contact.setUser(user);
            this.contactRepository.save(contact);
            session.setAttribute("message", new Message("seccussfully updated your contact ", "success"));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Contact Name : "+contact.getName());
        System.out.println("Contact Id : "+contact.getcId());
        return "redirect:/user/"+contact.getcId()+"/contact";
    }

    //profile heandler
    @GetMapping("/profile")
    public String yourProfile(Model model){
        model.addAttribute("title", "Profile Page");
        return "normal/profile";
    }

    //setting heandler
    @GetMapping("/setting")
    public String yourSetting(Model model){
        model.addAttribute("title", "Setting");
        return "normal/setting";
    }
}
