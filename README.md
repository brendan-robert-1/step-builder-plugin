This is the source code for the step builder plugin for the Intellij IDE.

Step Builder is a object creation pattern that is an extension of the traditional builder pattern. 
With a traditional Builder pattern we have a private constructor and an inner ```public static Builder``` class.
This builder class has an instance of the parent class which it calls setters on when clients call the various 
builder setter methods such as ```addFirstName(String firstName)```.

The traditional Builder pattern has a  ```build()``` method that returns the instance.
This method may be called at any time, which can cause invalid states of the returned instance. For instance, the
client may call the build method immediately without passing in any values, maybe this is acceptable, but often
is not. There is simply no way to guarantee state when the clients may call the build method at any time, and choose whether
or not to call the various builder setter methods.

Another issue with the traditional Builder pattern is that clients may be unfamiliar with your API, and get confused,
or frustrated with not knowing what builder setters to call, and in what order. The Step Builder pattern solves this
by holding the clients hand as they are building an instance of your class. The can only call the next method(s)
that you specify in the builder class. The first call ```setFirstName(String firstName)``` and then call ```setLastName(String lastName)```
and finally call ```build()```. This way, they shouldnt have an issue knowing what they are passing in and should never
accidentally set the first name to the last name field.

This plugin eases the necessary boilerplate that is associated with the step builder pattern. I believe that it is very valuable
when building objects with guaranteed state and ease of use.