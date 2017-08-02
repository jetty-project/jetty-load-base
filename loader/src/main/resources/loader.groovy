import org.mortbay.jetty.load.generator.Resource

return new Resource("/",
        new Resource("/css/bootstrap.min.css"),
        new Resource("/css/bootstrap-responsive.min.css"),
        new Resource("/lib/jquery.min.js"),
        new Resource("/lib/bootstrap.min.js"),
        new Resource("/lib/angular.min.js"),
        new Resource("/app.js"),
        new Resource(
                new Resource("/api/contact/search/name?q=12",
                        new Resource("/api/contact/12",
                                new Resource("/api/contact/120",
                                        new Resource("/api/contact/121")))),
                new Resource("/api/contact/search/name?q=13",
                        new Resource("/api/contact/13",
                                new Resource("/api/contact/130",
                                        new Resource("/api/contact/131")))))
)
