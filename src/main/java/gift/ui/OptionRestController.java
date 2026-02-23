package gift.ui;

import gift.application.CreateOptionRequest;
import gift.application.OptionService;
import gift.model.Option;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/options")
@RestController
public class OptionRestController {
    private final OptionService optionService;

    public OptionRestController(final OptionService optionService) {
        this.optionService = optionService;
    }

    @PostMapping
    public Option create(@RequestBody final CreateOptionRequest request) {
        return optionService.create(request);
    }

    @GetMapping
    public List<Option> retrieve() {
        return optionService.retrieve();
    }
}