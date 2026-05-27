package com.nakpom.exception

class EmailAlreadyExistsException(email: String) :
    RuntimeException("Account with email '$email' already exists")
