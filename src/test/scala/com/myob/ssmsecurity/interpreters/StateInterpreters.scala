package com.myob.ssmsecurity.interpreters

import cats.data.State
import cats.implicits._
import com.myob.ssmsecurity.algebras.{KafkaAclsAlg, KafkaTopicsAlg, KafkaUsersAlg, LogAlg}
import com.myob.ssmsecurity.models.{StoredUser, Topic, User, UserName}
import kafka.security.auth.{Acl, Resource}

object StateInterpreters {

  case class SystemState(
                          usersAdded: List[User] = List(),
                          usersUpdated: List[User] = List(),
                          userNamesRemoved: List[UserName] = List(),
                          topics: List[Topic] = List(),
                          infos: List[String] = List(),
                          errors: List[String] = List(),
                          aclsAdded: Map[Resource, Set[Acl]] = Map(),
                          aclsRemoved: Map[Resource, Set[Acl]] = Map())

  type TestProgram[A] = State[SystemState, A]

  class KafkaTopicsAlgState(topicNames: Set[String]) extends KafkaTopicsAlg[TestProgram] {

    override def createTopic(topic: Topic): TestProgram[Unit] = State { st =>
      (st.copy(topics = topic :: st.topics), ())
    }

    override def getTopicNames: TestProgram[Set[String]] = State.pure(topicNames)
  }

  class LogAlgState extends LogAlg[TestProgram] {
    override def info(message: String): TestProgram[Unit] = State { st =>
      (st.copy(infos = message :: st.infos), ())
    }
    override def error(message: String): TestProgram[Unit] = State { st =>
      (st.copy(errors = message :: st.errors), ())
    }
  }

  class KafkaAclsAlgState(acls: Map[Resource, Set[Acl]]) extends KafkaAclsAlg[TestProgram] {
    override def getKafkaAcls: TestProgram[Map[Resource, Set[Acl]]] = State.pure(acls)

    override def addAcls(acls: Map[Resource, Set[Acl]]): TestProgram[Unit] = State { st =>
      (st.copy(aclsAdded = st.aclsAdded.combine(acls)), ())
    }

    override def removeAcls(acls: Map[Resource, Set[Acl]]): TestProgram[Unit] = State { st =>
      (st.copy(aclsRemoved = st.aclsRemoved.combine(acls)), ())
    }
  }

  class KafkaUsersAlgState(storedUsers: Set[StoredUser]) extends KafkaUsersAlg[TestProgram] {
    override def createUser(user: User): TestProgram[Unit] = State { st =>
      (st.copy(usersAdded = user :: st.usersAdded), ())
    }

    override def updateUser(user: User): TestProgram[Unit] = State { st =>
      (st.copy(usersUpdated = user :: st.usersUpdated), ())
    }

    override def getStoredUsers: TestProgram[Set[StoredUser]] = State.pure(storedUsers)

    override def removeUser(userName: UserName): TestProgram[Unit] = State { st =>
      (st.copy(userNamesRemoved = userName :: st.userNamesRemoved), ())
    }
  }
}
