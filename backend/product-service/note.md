```
product-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/productservice/
│   │   │   ├── application/              
│   │   │   │   └── web/
│   │   │   │       ├── controller/       
│   │   │   │       ├── dto/              
│   │   │   │       └── mapper/          
│   │   │   ├── domain/                   
│   │   │   │   ├── model/                # Product.java, Price.java
│   │   │   │   ├── ports/
│   │   │   │   │   ├── in/               # ProductUseCase.java (Interfaces)
│   │   │   │   │   └── out/              # ProductRepositoryPort.java
│   │   │   │   └── service/              # ProductServiceImpl.java
│   │   │   ├── infrastructure/           
│   │   │   │   ├── persistence/
│   │   │   │   │   └── mongo/            # MongoRepository & EntityMappers
│   │   │   │   └── messaging/            # KafkaProducerAdapter.java
│   │   │   └── config/                   # 🔌 THE GLUE (Spring Beans)
│   │   │       └── BeanConfiguration.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/                             # 🧪 Logic & Integration tests
└── pom.xml
```
```curl -H "Authorization: Bearer <your-jwt>" http://localhost:8080/products -X POST -d '{"name":"test"}'```


```
/opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```