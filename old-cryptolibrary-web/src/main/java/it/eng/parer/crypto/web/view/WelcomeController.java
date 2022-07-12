package it.eng.parer.crypto.web.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import it.eng.parer.crypto.service.helper.ViewHelper;

@Controller("/")
public class WelcomeController {

    @Autowired
    Environment env;

    @Autowired
    BuildProperties buildProperties;

    @Autowired
    ViewHelper viewHelper;

    @GetMapping("/")
    public ModelAndView main(Model model) {

        build(model);

        return new ModelAndView("index");
    }

    @GetMapping("/admin")
    public ModelAndView admin(Model model) {

        build(model);

        return new ModelAndView("admin");
    }

    private void build(Model model) {
        // application properties
        viewHelper.convertAppPropertiesAsMap(model);

        // app infos
        this.infos(model);
    }

    private void infos(Model model) {
        model.addAttribute("version", env.getProperty("git.build.version"));
        model.addAttribute("builddate", env.getProperty("git.commit.time"));
        model.addAttribute("engcryptolibrary", buildProperties.get("eng-cryptolibrary"));

    }

}
