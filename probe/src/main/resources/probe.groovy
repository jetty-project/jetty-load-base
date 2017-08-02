import org.mortbay.jetty.load.generator.Resource

return new Resource("/",
        new Resource("/css/bootstrap.min.css"),
        new Resource("/css/bootstrap-responsive.min.css"),
        new Resource("/lib/jquery.min.js"),
        new Resource("/lib/bootstrap.min.js"),
        new Resource("/lib/angular.min.js"),
        new Resource("/app.js")
)
