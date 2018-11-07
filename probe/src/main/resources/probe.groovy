import org.mortbay.jetty.load.generator.Resource

return new Resource("/test/hello",
                    new Resource( "/test/postServlet/foo").method( "POST" ).requestLength(20000)
)
